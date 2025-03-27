/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.querysamplers;

import static org.opensearch.searchrelevance.plugin.Constants.UBI_QUERIES_INDEX_NAME;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.search.SearchScrollRequest;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.Scroll;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.searchrelevance.plugin.engines.OpenSearchEngine;
import org.opensearch.transport.client.node.NodeClient;

/**
 * An implementation of {@link AbstractQuerySampler} that uses PPTSS sampling.
 * See https://opensourceconnections.com/blog/2022/10/13/how-to-succeed-with-explicit-relevance-evaluation-using-probability-proportional-to-size-sampling/
 * for more information on PPTSS.
 */
public class ProbabilityProportionalToSizeQuerySampler extends AbstractQuerySampler {

    public static final String NAME = "pptss";

    private static final Logger LOGGER = LogManager.getLogger(ProbabilityProportionalToSizeQuerySampler.class);

    private final NodeClient client;
    private final ProbabilityProportionalToSizeQuerySamplerParameters parameters;

    /**
     * Creates a new PPTSS sampler.
     * @param client The OpenSearch {@link NodeClient client}.
     * @param parameters The {@link ProbabilityProportionalToSizeQuerySamplerParameters parameters} for the sampling.
     */
    public ProbabilityProportionalToSizeQuerySampler(
        final OpenSearchEngine openSearchEngine,
        final NodeClient client,
        final ProbabilityProportionalToSizeQuerySamplerParameters parameters
    ) {
        super(openSearchEngine);
        this.client = client;
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String sample() throws Exception {

        // Get queries from the UBI queries index.
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(10000);

        final Scroll scroll = new Scroll(TimeValue.timeValueMinutes(10L));

        final SearchRequest searchRequest = new SearchRequest(UBI_QUERIES_INDEX_NAME);
        searchRequest.scroll(scroll);
        searchRequest.source(searchSourceBuilder);

        // TODO: Don't use .get()
        SearchResponse searchResponse = client.search(searchRequest).get();

        String scrollId = searchResponse.getScrollId();
        SearchHit[] searchHits = searchResponse.getHits().getHits();

        final Collection<String> userQueries = new ArrayList<>();

        while (searchHits != null && searchHits.length > 0) {

            for (final SearchHit hit : searchHits) {
                final Map<String, Object> fields = hit.getSourceAsMap();
                userQueries.add(fields.get("user_query").toString());
                // LOGGER.info("user queries count: {} user query: {}", userQueries.size(), fields.get("user_query").toString());
            }

            final SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
            scrollRequest.scroll(scroll);

            // TODO: Don't use .get()
            searchResponse = client.searchScroll(scrollRequest).get();

            // scrollId = searchResponse.getScrollId();
            searchHits = searchResponse.getHits().getHits();

        }

        // LOGGER.info("User queries found: {}", userQueries);

        final Map<String, Long> weights = new HashMap<>();
        final Map<String, Double> normalizedWeights = new HashMap<>();
        final Map<String, Double> cumulativeWeights = new HashMap<>();
        final Map<String, Long> querySet = new HashMap<>();

        // Increment the weight for each user query.
        for (final String userQuery : userQueries) {
            weights.merge(userQuery, 1L, Long::sum);
        }

        // The total number of queries will be used to normalize the weights.
        final long countOfQueries = userQueries.size();

        for (final String userQuery : weights.keySet()) {
            // Calculate the normalized weights by dividing by the total number of queries.
            normalizedWeights.put(userQuery, weights.get(userQuery) / (double) countOfQueries);
        }

        // Ensure all normalized weights sum to 1.
        final double sumOfNormalizedWeights = normalizedWeights.values().stream().reduce(0.0, Double::sum);
        if (!compare(1.0, sumOfNormalizedWeights)) {
            throw new RuntimeException("Summed normalized weights do not equal 1.0: Actual value: " + sumOfNormalizedWeights);
        } else {
            LOGGER.debug("Summed normalized weights sum to {}", sumOfNormalizedWeights);
        }

        // Create weight "ranges" for each query.
        double lastWeight = 0;
        for (final String userQuery : normalizedWeights.keySet()) {
            lastWeight = normalizedWeights.get(userQuery) + lastWeight;
            cumulativeWeights.put(userQuery, lastWeight);
        }

        // The last weight should be 1.0.
        if (!compare(lastWeight, 1.0)) {
            throw new RuntimeException("The sum of the cumulative weights does not equal 1.0: Actual value: " + lastWeight);
        }

        final UniformRealDistribution uniform = new UniformRealDistribution(0, 1);

        for (int i = 1; i <= parameters.getQuerySetSize(); i++) {

            final double r = uniform.sample();

            for (final String userQuery : cumulativeWeights.keySet()) {

                final double cumulativeWeight = cumulativeWeights.get(userQuery);
                if (cumulativeWeight >= r) {
                    // This ignores duplicate queries.
                    querySet.put(userQuery, weights.get(userQuery));
                    break;
                }

            }

        }

        final String querySetId = UUID.randomUUID().toString();

        return indexQuerySet(querySetId, parameters.getName(), parameters.getDescription(), parameters.getSampling(), querySet);

    }

    private boolean compare(double a, double b) {
        return Math.abs(a - b) < 0.00001;
    }

}

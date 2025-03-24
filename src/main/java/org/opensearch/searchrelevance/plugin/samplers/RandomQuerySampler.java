/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.samplers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.ExistsQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.opensearch.index.query.functionscore.RandomScoreFunctionBuilder;
import org.opensearch.index.query.functionscore.ScoreFunctionBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.collapse.CollapseBuilder;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.judgments.opensearch.OpenSearchHelper;

/**
 * A sampler that randomly selects a given number of queries.
 */
public class RandomQuerySampler extends AbstractQuerySampler {

    private static final Logger LOGGER = LogManager.getLogger(RandomQuerySampler.class);

    public static final String NAME = "random";

    private final RandomQuerySamplerParameters parameters;

    public RandomQuerySampler(final OpenSearchHelper openSearchHelper, final RandomQuerySamplerParameters parameters) {
        super(openSearchHelper);
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String sample() throws IOException {

        final String userQueryField = "user_query";

        final long seed = System.currentTimeMillis();

        final RandomScoreFunctionBuilder randomScoreFunction = ScoreFunctionBuilders.randomFunction(); // .setField(userQueryField).seed(seed);

        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
            QueryBuilders.matchAllQuery(),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder[] {
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(randomScoreFunction) }
        );

        final ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(userQueryField);
        final TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(userQueryField, "");

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
            .must(existsQueryBuilder)
            .must(functionScoreQueryBuilder)
            .mustNot(termQueryBuilder);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder)
            .collapse(new CollapseBuilder(userQueryField))
            .size(parameters.getQuerySetSize());

        final SearchRequest searchRequest = new SearchRequest(Constants.UBI_QUERIES_INDEX_NAME).source(searchSourceBuilder);

        openSearchHelper.getClient().search(searchRequest, new ActionListener<>() {

            @Override
            public void onResponse(SearchResponse searchResponse) {

                final Map<String, Long> querySet = new HashMap<>();

                // These are UBI queries.
                for (SearchHit hit : searchResponse.getHits().getHits()) {

                    final Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                    LOGGER.info("Hit ID: " + hit.getId());
                    LOGGER.info("Hit Score: " + hit.getScore());
                    LOGGER.info("Hit Source: " + sourceAsMap);

                    // Access individual properties from sourceAsMap
                    final String userQuery = (String) sourceAsMap.get(userQueryField);

                    if (userQuery != null) {
                        LOGGER.info("\t User Query: " + userQuery);
                    }

                    // TODO: Form the query set.
                    // final long count = getUserQueryCount(hit.source().getUserQuery());
                    // LOGGER.info("Adding user query to query set: {} with frequency {}", hit.source().getUserQuery(), count);
                    // querySet.put(hit.source().getUserQuery(), count);

                }

            }

            @Override
            public void onFailure(Exception ex) {
                LOGGER.error("Error executing search request: " + ex.getMessage(), ex);
            }

        });

        // TODO: Index the query set and return its ID.
        return "query-set-id";

    }

}

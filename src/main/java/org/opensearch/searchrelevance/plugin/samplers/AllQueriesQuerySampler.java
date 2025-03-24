/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.samplers;

import static org.opensearch.searchrelevance.plugin.Constants.UBI_QUERIES_INDEX_NAME;

import java.util.HashMap;
import java.util.Map;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.searchrelevance.plugin.judgments.opensearch.OpenSearchHelper;
import org.opensearch.transport.client.node.NodeClient;

/**
 * An implementation of {@link AbstractQuerySampler} that uses all UBI queries without any sampling.
 */
public class AllQueriesQuerySampler extends AbstractQuerySampler {

    public static final String NAME = "none";

    private final NodeClient client;
    private final AllQueriesQuerySamplerParameters parameters;

    /**
     * Creates a new sampler.
     * @param client The OpenSearch {@link NodeClient client}.
     */
    public AllQueriesQuerySampler(
        final OpenSearchHelper openSearchHelper,
        final NodeClient client,
        final AllQueriesQuerySamplerParameters parameters
    ) {
        super(openSearchHelper);
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
        // TODO: This needs to use scroll or something else.
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(parameters.getQuerySetSize());

        final SearchRequest searchRequest = new SearchRequest(UBI_QUERIES_INDEX_NAME).source(searchSourceBuilder);

        // TODO: Don't use .get()
        final SearchResponse searchResponse = client.search(searchRequest).get();

        final Map<String, Long> queries = new HashMap<>();

        for (final SearchHit hit : searchResponse.getHits().getHits()) {

            final Map<String, Object> fields = hit.getSourceAsMap();
            queries.merge(fields.get("user_query").toString(), 1L, Long::sum);

            // Will be useful for paging once implemented.
            if (queries.size() > parameters.getQuerySetSize()) {
                break;
            }

        }

        return indexQuerySet(client, parameters.getName(), parameters.getDescription(), parameters.getSampling(), queries);

    }

}

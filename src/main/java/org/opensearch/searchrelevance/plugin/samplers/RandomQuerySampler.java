/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.samplers;

import java.io.IOException;

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

        final String USER_QUERY_FIELD = "user_query";

        final long seed = System.currentTimeMillis();

        final RandomScoreFunctionBuilder randomScoreFunction = ScoreFunctionBuilders.randomFunction().seed(seed);

        final FunctionScoreQueryBuilder functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
            QueryBuilders.matchAllQuery(),
            new FunctionScoreQueryBuilder.FilterFunctionBuilder[] {
                new FunctionScoreQueryBuilder.FilterFunctionBuilder(randomScoreFunction) }
        );

        final ExistsQueryBuilder existsQueryBuilder = QueryBuilders.existsQuery(USER_QUERY_FIELD);
        final TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery(USER_QUERY_FIELD, "");

        final BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
            .must(existsQueryBuilder)
            .must(functionScoreQueryBuilder)
            .mustNot(termQueryBuilder);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQueryBuilder)
            .collapse(new CollapseBuilder(USER_QUERY_FIELD))
            .size(parameters.getQuerySetSize());

        final SearchRequest searchRequest = new SearchRequest(Constants.UBI_QUERIES_INDEX_NAME).source(searchSourceBuilder);

        openSearchHelper.getClient().search(searchRequest, new ActionListener<>() {

            @Override
            public void onResponse(SearchResponse searchResponse) {

                LOGGER.info("Total hits: " + searchResponse.getHits().getTotalHits().value());
                // Process searchResponse.getHits() here.

                // TODO: These are UBI queries.

                // final Map<String, Long> querySet = new HashMap<>();
                //
                // searchResponse.hits().hits().forEach(hit -> {
                // final long count = getUserQueryCount(hit.source().getUserQuery());
                // LOGGER.info("Adding user query to query set: {} with frequency {}", hit.source().getUserQuery(), count);
                // querySet.put(hit.source().getUserQuery(), count);
                // });

            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.error("Error executing search request: " + e.getMessage());
            }

        });

        // TODO: Index the query set and return its ID.
        return "query-set-id";

    }

}

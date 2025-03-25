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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.aggregations.AggregationBuilder;
import org.opensearch.search.aggregations.AggregationBuilders;
import org.opensearch.search.aggregations.bucket.terms.Terms;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.judgments.opensearch.OpenSearchHelper;
import org.opensearch.searchrelevance.plugin.utils.TimeUtils;

/**
 * A sampler that selects the top N queries.
 */
public class TopNQuerySampler extends AbstractQuerySampler {

    private static final Logger LOGGER = LogManager.getLogger(TopNQuerySampler.class);

    public static final String NAME = "topn";

    private static final String USER_QUERY_FIELD = "user_query";
    private final TopNQuerySamplerParameters parameters;

    public TopNQuerySampler(final OpenSearchHelper openSearchHelper, final TopNQuerySamplerParameters parameters) {
        super(openSearchHelper);
        this.parameters = parameters;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String sample() throws IOException {

        final Map<String, Long> querySet = new HashMap<>();

        final AggregationBuilder userQueryAggregation = AggregationBuilders.terms("By_User_Query")
            .field(USER_QUERY_FIELD)
            .size(parameters.getQuerySetSize());

        final BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
            .must(QueryBuilders.existsQuery(USER_QUERY_FIELD))
            .mustNot(QueryBuilders.termQuery(USER_QUERY_FIELD, ""));

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder().query(boolQuery)
            .aggregation(userQueryAggregation)
            .from(0)
            .size(0);

        final SearchRequest searchRequest = new SearchRequest(Constants.UBI_QUERIES_INDEX_NAME).source(searchSourceBuilder);

        final SearchResponse searchResponse = openSearchHelper.getClient().search(searchRequest).actionGet();

        final Terms byUserQuery = searchResponse.getAggregations().get("By_User_Query");
        List<? extends Terms.Bucket> byActionBuckets = byUserQuery.getBuckets();

        for (Terms.Bucket bucket : byActionBuckets) {
            LOGGER.info("Adding user query to query set: {} with frequency {}", bucket.getKey().toString(), bucket.getDocCount());
            querySet.put(bucket.getKey().toString(), bucket.getDocCount());
        }

        LOGGER.info("Indexing query set containing {} queries.", querySet.size());

        // Index the query set and return its ID.
        openSearchHelper.createIndexIfNotExists(Constants.QUERY_SETS_INDEX_NAME, Constants.QUERY_SETS_INDEX_MAPPING);

        final String querySetId = UUID.randomUUID().toString();
        final String timestamp = TimeUtils.getTimestamp();

        final Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("timestamp", timestamp);
        jsonMap.put("description", parameters.getDescription());
        jsonMap.put("id", querySetId);
        jsonMap.put("name", parameters.getName());
        jsonMap.put("query_set_queries", querySet);
        jsonMap.put("sampling", "random");

        final IndexRequest indexRequest = new IndexRequest(Constants.QUERY_SETS_INDEX_NAME).id(querySetId).source(jsonMap);

        openSearchHelper.getClient().index(indexRequest).actionGet();

        return querySetId;

    }

}

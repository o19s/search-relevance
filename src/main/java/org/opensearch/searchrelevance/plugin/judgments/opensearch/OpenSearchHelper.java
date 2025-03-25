/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.judgments.opensearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.admin.indices.create.CreateIndexRequest;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.opensearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.WrapperQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.judgments.model.ClickthroughRate;
import org.opensearch.searchrelevance.plugin.judgments.model.Judgment;
import org.opensearch.searchrelevance.plugin.judgments.model.SearchConfiguration;
import org.opensearch.searchrelevance.plugin.judgments.model.ubi.query.UbiQuery;
import org.opensearch.searchrelevance.plugin.utils.TimeUtils;
import org.opensearch.transport.client.Client;

import com.google.gson.Gson;

/**
 * Functionality for interacting with OpenSearch.
 * TODO: Move these functions out of this class.
 */
public class OpenSearchHelper {

    private static final Logger LOGGER = LogManager.getLogger(OpenSearchHelper.class.getName());

    private final Client client;
    private final Gson gson = new Gson();

    // Used to cache the query ID->user_query to avoid unnecessary lookups to OpenSearch.
    private static final Map<String, String> userQueryCache = new HashMap<>();

    public OpenSearchHelper(final Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    /**
     * Gets the user query for a given query ID.
     * @param queryId The query ID.
     * @return The user query.
     * @throws IOException Thrown when there is a problem accessing OpenSearch.
     */
    public String getUserQuery(final String queryId) throws Exception {

        // If it's in the cache just get it and return it.
        if (userQueryCache.containsKey(queryId)) {
            return userQueryCache.get(queryId);
        }

        // Cache it and return it.
        final UbiQuery ubiQuery = getQueryFromQueryId(queryId);

        // ubiQuery will be null if the query does not exist.
        if (ubiQuery != null) {

            userQueryCache.put(queryId, ubiQuery.getUserQuery());
            return ubiQuery.getUserQuery();

        } else {

            return null;

        }

    }

    /**
     * Gets the query object for a given query ID.
     * @param queryId The query ID.
     * @return A {@link UbiQuery} object for the given query ID.
     * @throws Exception Thrown if the query cannot be retrieved.
     */
    public UbiQuery getQueryFromQueryId(final String queryId) throws Exception {

        LOGGER.debug("Getting query from query ID {}", queryId);

        final String query = "{\"match\": {\"query_id\": \"" + queryId + "\" }}";
        final WrapperQueryBuilder qb = QueryBuilders.wrapperQuery(query);

        // The query_id should be unique anyway, but we are limiting it to a single result anyway.
        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qb);
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(1);

        final String[] indexes = { Constants.UBI_QUERIES_INDEX_NAME };

        final SearchRequest searchRequest = new SearchRequest(indexes, searchSourceBuilder);
        final SearchResponse response = client.search(searchRequest).get();

        // If this does not return a query then we cannot calculate the judgments. Each even should have a query associated with it.
        if (response.getHits().getHits() != null & response.getHits().getHits().length > 0) {

            final SearchHit hit = response.getHits().getHits()[0];
            return gson.fromJson(hit.getSourceAsString(), UbiQuery.class);

        } else {

            LOGGER.warn("No query exists for query ID {} to calculate judgments.", queryId);
            return null;

        }

    }

    private Collection<String> getQueryIdsHavingUserQuery(final String userQuery) throws Exception {

        final String query = "{\"match\": {\"user_query\": \"" + userQuery + "\" }}";
        final WrapperQueryBuilder qb = QueryBuilders.wrapperQuery(query);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qb);

        final String[] indexes = { Constants.UBI_QUERIES_INDEX_NAME };

        final SearchRequest searchRequest = new SearchRequest(indexes, searchSourceBuilder);
        final SearchResponse response = client.search(searchRequest).get();

        final Collection<String> queryIds = new ArrayList<>();

        for (final SearchHit hit : response.getHits().getHits()) {
            final String queryId = hit.getSourceAsMap().get("query_id").toString();
            queryIds.add(queryId);
        }

        return queryIds;

    }

    public long getCountOfQueriesForUserQueryHavingResultInRankR(final String userQuery, final String objectId, final int rank)
        throws Exception {

        long countOfTimesShownAtRank = 0;

        // Get all query IDs matching this user query.
        final Collection<String> queryIds = getQueryIdsHavingUserQuery(userQuery);

        // For each query ID, get the events with action_name = "impression" having a match on objectId and rank (position).
        for (final String queryId : queryIds) {

            final String query = "{\n"
                + "    \"bool\": {\n"
                + "      \"must\": [\n"
                + "          {\n"
                + "            \"term\": {\n"
                + "              \"query_id\": \""
                + queryId
                + "\"\n"
                + "            }\n"
                + "          },\n"
                + "          {\n"
                + "            \"term\": {\n"
                + "              \"action_name\": \"impression\"\n"
                + "            }\n"
                + "          },\n"
                + "          {\n"
                + "            \"term\": {\n"
                + "              \"event_attributes.position.ordinal\": \""
                + rank
                + "\"\n"
                + "            }\n"
                + "          },\n"
                + "          {\n"
                + "            \"term\": {\n"
                + "              \"event_attributes.object.object_id\": \""
                + objectId
                + "\"\n"
                + "            }\n"
                + "          }\n"
                + "        ]\n"
                + "      }\n"
                + "    }";

            final WrapperQueryBuilder qb = QueryBuilders.wrapperQuery(query);

            final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(qb);
            searchSourceBuilder.trackTotalHits(true);
            searchSourceBuilder.size(0);

            final String[] indexes = { Constants.UBI_EVENTS_INDEX_NAME };

            final SearchRequest searchRequest = new SearchRequest(indexes, searchSourceBuilder);
            final SearchResponse response = client.search(searchRequest).get();

            // Won't be null as long as trackTotalHits is true.
            if (response.getHits().getTotalHits() != null) {
                countOfTimesShownAtRank += response.getHits().getTotalHits().value();
            }

        }

        LOGGER.debug("Count of {} having {} at rank {} = {}", userQuery, objectId, rank, countOfTimesShownAtRank);

        if (countOfTimesShownAtRank > 0) {
            LOGGER.debug("Count of {} having {} at rank {} = {}", userQuery, objectId, rank, countOfTimesShownAtRank);
        }

        return countOfTimesShownAtRank;

    }

    /**
     * Index the rank-aggregated clickthrough values.
     * @param rankAggregatedClickThrough A map of position to clickthrough values.
     * @throws IOException Thrown when there is a problem accessing OpenSearch.
     */
    public void indexRankAggregatedClickthrough(final Map<Integer, Double> rankAggregatedClickThrough) throws Exception {

        if (!rankAggregatedClickThrough.isEmpty()) {

            createIndexIfNotExists(Constants.COEC_RANK_AGGREGATED_CTR_INDEX_NAME, Constants.COEC_RANK_AGGREGATED_CTR_INDEX_MAPPING);

            final BulkRequest request = new BulkRequest();

            for (final int position : rankAggregatedClickThrough.keySet()) {

                final Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("position", position);
                jsonMap.put("ctr", rankAggregatedClickThrough.get(position));

                final IndexRequest indexRequest = new IndexRequest(Constants.COEC_RANK_AGGREGATED_CTR_INDEX_NAME).id(
                    UUID.randomUUID().toString()
                ).source(jsonMap);

                request.add(indexRequest);

            }

            client.bulk(request).get();

        }

    }

    /**
     * Index the clickthrough rates.
     * @param clickthroughRates A map of query IDs to a collection of {@link ClickthroughRate} objects.
     * @throws IOException Thrown when there is a problem accessing OpenSearch.
     */
    public void indexClickthroughRates(final Map<String, Set<ClickthroughRate>> clickthroughRates) throws Exception {

        if (!clickthroughRates.isEmpty()) {

            final BulkRequest request = new BulkRequest();

            for (final String userQuery : clickthroughRates.keySet()) {

                for (final ClickthroughRate clickthroughRate : clickthroughRates.get(userQuery)) {

                    final Map<String, Object> jsonMap = new HashMap<>();
                    jsonMap.put("user_query", userQuery);
                    jsonMap.put("clicks", clickthroughRate.getClicks());
                    jsonMap.put("events", clickthroughRate.getImpressions());
                    jsonMap.put("ctr", clickthroughRate.getClickthroughRate());
                    jsonMap.put("object_id", clickthroughRate.getObjectId());

                    final IndexRequest indexRequest = new IndexRequest(Constants.COEC_CTR_INDEX_NAME).id(UUID.randomUUID().toString())
                        .source(jsonMap);

                    request.add(indexRequest);

                }

            }

            client.bulk(request, new ActionListener<>() {

                @Override
                public void onResponse(BulkResponse bulkItemResponses) {
                    if (bulkItemResponses.hasFailures()) {
                        LOGGER.error("Clickthrough rates were not all successfully indexed: {}", bulkItemResponses.buildFailureMessage());
                    } else {
                        LOGGER.debug("Clickthrough rates has been successfully indexed.");
                    }
                }

                @Override
                public void onFailure(Exception ex) {
                    LOGGER.error("Indexing the clickthrough rates failed.", ex);
                }

            });

        }

    }

    /**
     * Index the judgments.
     * @param judgments A collection of {@link Judgment judgments}.
     * @throws IOException Thrown when there is a problem accessing OpenSearch.
     * @return The ID of the indexed judgments.
     */
    public String indexJudgments(final Collection<Judgment> judgments) throws Exception {

        final String judgmentsId = UUID.randomUUID().toString();
        final String timestamp = TimeUtils.getTimestamp();

        final BulkRequest bulkRequest = new BulkRequest();

        for (final Judgment judgment : judgments) {

            final Map<String, Object> j = judgment.getJudgmentAsMap();
            j.put("judgments_id", judgmentsId);
            j.put("timestamp", timestamp);

            final IndexRequest indexRequest = new IndexRequest(Constants.JUDGMENTS_INDEX_NAME).id(UUID.randomUUID().toString()).source(j);

            bulkRequest.add(indexRequest);

        }

        // TODO: Don't use .get()
        client.bulk(bulkRequest).get();

        return judgmentsId;

    }

    public void createIndexIfNotExists(final String indexName, final String indexMapping) {

        final IndicesExistsRequest indicesExistsRequest = new IndicesExistsRequest(indexName);

        client.admin().indices().exists(indicesExistsRequest, new ActionListener<>() {

            @Override
            public void onResponse(IndicesExistsResponse indicesExistsResponse) {

                if (!indicesExistsResponse.isExists()) {

                    final CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName).mapping(indexMapping);

                    client.admin().indices().create(createIndexRequest, new ActionListener<>() {

                        @Override
                        public void onResponse(CreateIndexResponse createIndexResponse) {
                            LOGGER.info("{} index created.", indexName);
                        }

                        @Override
                        public void onFailure(Exception ex) {
                            LOGGER.error("Unable to create the {} index.", indexName, ex);
                        }

                    });

                }

            }

            @Override
            public void onFailure(Exception ex) {
                LOGGER.error("Unable to determine if {} index exists.", indexName, ex);
            }

        });

    }

    public long getUserQueryCount(final String userQuery) {

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("user_query", userQuery));
        searchSourceBuilder.size(0);
        searchSourceBuilder.trackTotalHits(true);

        final SearchRequest searchRequest = new SearchRequest(Constants.UBI_QUERIES_INDEX_NAME);
        searchRequest.source(searchSourceBuilder);

        final SearchResponse searchResponse = client.search(searchRequest)
            .actionGet(TimeValue.timeValueSeconds(5).millis(), TimeUnit.MILLISECONDS);

        final SearchHits hits = searchResponse.getHits();

        return hits.getTotalHits().value();

    }

    public String indexSearchConfiguration(final SearchConfiguration searchConfiguration) {

        final String searchConfigurationId = UUID.randomUUID().toString();

        final Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("search_configuration_name", searchConfiguration.getSearchConfigurationName());
        jsonMap.put("query_body", searchConfiguration.getQueryBody());

        final IndexRequest indexRequest = new IndexRequest(Constants.SEARCH_CONFIG_INDEX_NAME).id(searchConfigurationId).source(jsonMap);

        client.index(indexRequest).actionGet();

        return searchConfigurationId;

    }

}

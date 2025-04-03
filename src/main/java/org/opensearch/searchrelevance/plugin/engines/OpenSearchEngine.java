/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.engines;

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
import org.opensearch.action.delete.DeleteRequest;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.WrapperQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.FieldSortBuilder;
import org.opensearch.search.sort.SortOrder;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.model.ClickthroughRate;
import org.opensearch.searchrelevance.plugin.model.GetJudgmentsRequest;
import org.opensearch.searchrelevance.plugin.model.GetQuerySetsRequest;
import org.opensearch.searchrelevance.plugin.model.GetSearchConfigurationsRequest;
import org.opensearch.searchrelevance.plugin.model.Judgment;
import org.opensearch.searchrelevance.plugin.model.SearchConfiguration;
import org.opensearch.searchrelevance.plugin.model.ubi.query.UbiQuery;
import org.opensearch.searchrelevance.plugin.utils.JsonUtils;
import org.opensearch.searchrelevance.plugin.utils.TimeUtils;
import org.opensearch.transport.client.Client;

/**
 * Functionality for interacting with OpenSearch.
 */
public class OpenSearchEngine implements SearchEngine {

    private static final Logger LOGGER = LogManager.getLogger(OpenSearchEngine.class.getName());

    private final Client client;

    // Used to cache the query ID->user_query to avoid unnecessary lookups to OpenSearch.
    private static final Map<String, String> userQueryCache = new HashMap<>();

    public OpenSearchEngine(final Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }

    @Override
    public String getUserQuery(final String queryId) throws Exception {

        // If it's in the cache, just get it and return it.
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

    @Override
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

        // TODO: Don't use .get()
        final SearchResponse response = client.search(searchRequest).get();

        // If this does not return a query, then we cannot calculate the judgments. Each even should have a query associated with it.
        if (response.getHits().getHits() != null & response.getHits().getHits().length > 0) {

            final SearchHit hit = response.getHits().getHits()[0];
            return JsonUtils.fromJson(hit.getSourceAsString(), UbiQuery.class);

        } else {

            LOGGER.warn("No query exists for query ID {} to calculate judgments.", queryId);
            return null;

        }

    }

    @Override
    public Collection<String> getQueryIdsHavingUserQuery(final String userQuery) throws Exception {

        final String query = "{\"match\": {\"user_query\": \"" + userQuery + "\" }}";
        final WrapperQueryBuilder qb = QueryBuilders.wrapperQuery(query);

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qb);

        final String[] indexes = { Constants.UBI_QUERIES_INDEX_NAME };

        final SearchRequest searchRequest = new SearchRequest(indexes, searchSourceBuilder);

        // TODO: Don't use .get()
        final SearchResponse response = client.search(searchRequest).get();

        final Collection<String> queryIds = new ArrayList<>();

        for (final SearchHit hit : response.getHits().getHits()) {
            final String queryId = hit.getSourceAsMap().get("query_id").toString();
            queryIds.add(queryId);
        }

        return queryIds;

    }

    @Override
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

    @Override
    public void indexRankAggregatedClickthrough(final Map<Integer, Double> rankAggregatedClickThrough) {

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

            client.bulk(request, new ActionListener<>() {

                @Override
                public void onResponse(BulkResponse bulkItemResponses) {
                    if (bulkItemResponses.hasFailures()) {
                        LOGGER.error(
                            "Rank-aggregated clickthrough were not all successfully indexed: {}",
                            bulkItemResponses.buildFailureMessage()
                        );
                    } else {
                        LOGGER.debug("ank-aggregated clickthrough has been successfully indexed.");
                    }
                }

                @Override
                public void onFailure(Exception ex) {
                    LOGGER.error("Indexing the rank-aggregated clickthrough failed.", ex);
                }

            });

        }

    }

    @Override
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

    @Override
    public String indexJudgments(final Collection<Judgment> judgments) throws Exception {

        final String judgmentsId = UUID.randomUUID().toString();
        final String timestamp = TimeUtils.getTimestamp();

        final BulkRequest bulkRequest = new BulkRequest();

        for (final Judgment judgment : judgments) {

            final Map<String, Object> j = judgment.getJudgmentAsMap();

            // Add these additional properties to the judgment.
            j.put("judgments_id", judgmentsId);
            j.put("timestamp", timestamp);

            final IndexRequest indexRequest = new IndexRequest(Constants.JUDGMENTS_INDEX_NAME).id(UUID.randomUUID().toString()).source(j);

            bulkRequest.add(indexRequest);

        }

        client.bulk(bulkRequest, new ActionListener<>() {

            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                if (bulkItemResponses.hasFailures()) {
                    LOGGER.error("Judgments were not all successfully indexed: {}", bulkItemResponses.buildFailureMessage());
                } else {
                    LOGGER.debug("Judgments have been successfully indexed.");
                }
            }

            @Override
            public void onFailure(Exception ex) {
                LOGGER.error("Indexing the judgments have failed.", ex);
            }
        });

        return judgmentsId;

    }

    @Override
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

    @Override
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

    @Override
    public void getQuerySets(final GetQuerySetsRequest getQuerySetsRequest, final ActionListener<SearchResponse> listener) {

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(getQuerySetsRequest.getSize());

        searchSourceBuilder.sort(
            new FieldSortBuilder("timestamp").order(SortOrder.fromString(getQuerySetsRequest.getSort().getTimestamp().getOrder()))
        );

        final SearchRequest searchRequest = new SearchRequest(Constants.QUERY_SETS_INDEX_NAME);
        searchRequest.source(searchSourceBuilder);

        client.search(searchRequest, listener);

    }

    @Override
    public void getJudgments(final GetJudgmentsRequest getJudgmentsRequest, final ActionListener<SearchResponse> listener) {

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(getJudgmentsRequest.getSize());

        searchSourceBuilder.sort(
            new FieldSortBuilder("timestamp").order(SortOrder.fromString(getJudgmentsRequest.getSort().getTimestamp().getOrder()))
        );

        final SearchRequest searchRequest = new SearchRequest(Constants.JUDGMENTS_INDEX_NAME);
        searchRequest.source(searchSourceBuilder);

        client.search(searchRequest, listener);

    }

    @Override
    public void deleteJudgment(final String judgmentId, final ActionListener<DeleteResponse> listener) {

        LOGGER.info("Deleting judgment with ID: {}", judgmentId);

        final DeleteRequest deleteRequest = new DeleteRequest(Constants.JUDGMENTS_INDEX_NAME).id(judgmentId);
        client.delete(deleteRequest, listener);

    }

    @Override
    public void deleteQuerySet(final String querySetId, final ActionListener<DeleteResponse> listener) {

        LOGGER.info("Deleting query set with ID: {}", querySetId);

        final DeleteRequest deleteRequest = new DeleteRequest(Constants.QUERY_SETS_INDEX_NAME).id(querySetId);
        client.delete(deleteRequest, listener);

    }

    @Override
    public void indexSearchConfiguration(
        final String searchConfigurationId,
        final SearchConfiguration searchConfiguration,
        ActionListener<IndexResponse> listener
    ) {

        final Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("search_configuration_name", searchConfiguration.getSearchConfigurationName());
        jsonMap.put("query_body", searchConfiguration.getQueryBody());
        jsonMap.put("id", searchConfigurationId);
        jsonMap.put("timestamp", TimeUtils.getTimestamp());

        final IndexRequest indexRequest = new IndexRequest(Constants.SEARCH_CONFIGURATIONS_INDEX_NAME).id(searchConfigurationId)
            .source(jsonMap);

        client.index(indexRequest, listener);

    }

    @Override
    public void deleteSearchConfiguration(final String searchConfigurationId, final ActionListener<DeleteResponse> listener) {

        LOGGER.info("Deleting search configuration with ID: {}", searchConfigurationId);

        final DeleteRequest deleteRequest = new DeleteRequest(Constants.SEARCH_CONFIGURATIONS_INDEX_NAME).id(searchConfigurationId);
        client.delete(deleteRequest, listener);

    }

    @Override
    public void getSearchConfigurations(
        final GetSearchConfigurationsRequest getSearchConfigurationsRequest,
        final ActionListener<SearchResponse> listener
    ) {

        final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchSourceBuilder.size(getSearchConfigurationsRequest.getSize());

        searchSourceBuilder.sort(
            new FieldSortBuilder("timestamp").order(
                SortOrder.fromString(getSearchConfigurationsRequest.getSort().getTimestamp().getOrder())
            )
        );

        final SearchRequest searchRequest = new SearchRequest(Constants.SEARCH_CONFIGURATIONS_INDEX_NAME);
        searchRequest.source(searchSourceBuilder);

        client.search(searchRequest, listener);

    }

}

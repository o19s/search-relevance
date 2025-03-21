/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.runners;

import static org.opensearch.searchrelevance.plugin.Constants.DASHBOARD_METRICS_INDEX_NAME;
import static org.opensearch.searchrelevance.plugin.Constants.QUERY_PLACEHOLDER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.opensearch.core.action.ActionListener;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.searchrelevance.plugin.metrics.DcgSearchMetric;
import org.opensearch.searchrelevance.plugin.metrics.NdcgSearchMetric;
import org.opensearch.searchrelevance.plugin.metrics.PrecisionSearchMetric;
import org.opensearch.searchrelevance.plugin.metrics.SearchMetric;
import org.opensearch.searchrelevance.plugin.utils.TimeUtils;
import org.opensearch.transport.client.Client;

/**
 * A {@link AbstractQuerySetRunner} for Amazon OpenSearch.
 */
public class OpenSearchQuerySetRunner extends AbstractQuerySetRunner {

    private static final Logger LOGGER = LogManager.getLogger(OpenSearchQuerySetRunner.class);

    /**
     * Creates a new query set runner
     *
     * @param client An OpenSearch {@link Client}.
     */
    public OpenSearchQuerySetRunner(final Client client) {
        super(client);
    }

    @Override
    public QuerySetRunResult run(
        final String querySetId,
        final String judgmentsId,
        final String index,
        final String searchPipeline,
        final String idField,
        final String query,
        final int k,
        final double threshold
    ) throws Exception {

        final Collection<Map<String, Long>> querySet = getQuerySet(querySetId);
        LOGGER.info("Found {} queries in query set {}", querySet.size(), querySetId);

        try {

            // The results of each query.
            final List<QueryResult> queryResults = new ArrayList<>();

            for (Map<String, Long> queryMap : querySet) {

                // Loop over each query in the map and run each one.
                for (final String userQuery : queryMap.keySet()) {

                    // Replace the query placeholder with the user query.
                    final String parsedQuery = query.replace(QUERY_PLACEHOLDER, userQuery);

                    // Build the query from the one that was passed in.
                    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

                    searchSourceBuilder.query(QueryBuilders.wrapperQuery(parsedQuery));
                    searchSourceBuilder.from(0);
                    searchSourceBuilder.size(k);

                    final String[] includeFields = new String[] { idField };
                    final String[] excludeFields = new String[] {};
                    searchSourceBuilder.fetchSource(includeFields, excludeFields);

                    // LOGGER.info(searchSourceBuilder.toString());

                    final SearchRequest searchRequest = new SearchRequest(index);
                    searchRequest.source(searchSourceBuilder);

                    if (searchPipeline != null) {
                        searchSourceBuilder.pipeline(searchPipeline);
                        searchRequest.pipeline(searchPipeline);
                    }

                    // This is to keep OpenSearch from rejecting queries.
                    // TODO: Look at using the Workload Management in 2.18.0.
                    Thread.sleep(50);

                    client.search(searchRequest, new ActionListener<>() {

                        @Override
                        public void onResponse(final SearchResponse searchResponse) {

                            final List<String> orderedDocumentIds = new ArrayList<>();

                            for (final SearchHit hit : searchResponse.getHits().getHits()) {

                                final String documentId;

                                if ("_id".equals(idField)) {
                                    documentId = hit.getId();
                                } else {
                                    // TODO: Need to check this field actually exists.
                                    documentId = hit.getSourceAsMap().get(idField).toString();
                                }

                                orderedDocumentIds.add(documentId);

                            }

                            try {

                                final RelevanceScores relevanceScores = getRelevanceScores(judgmentsId, userQuery, orderedDocumentIds, k);

                                // Calculate the metrics for this query.
                                final SearchMetric dcgSearchMetric = new DcgSearchMetric(k, relevanceScores.getRelevanceScores());
                                final SearchMetric ndcgSearchmetric = new NdcgSearchMetric(k, relevanceScores.getRelevanceScores());
                                final SearchMetric precisionSearchMetric = new PrecisionSearchMetric(
                                    k,
                                    threshold,
                                    relevanceScores.getRelevanceScores()
                                );

                                final Collection<SearchMetric> searchMetrics = List.of(
                                    dcgSearchMetric,
                                    ndcgSearchmetric,
                                    precisionSearchMetric
                                );

                                queryResults.add(
                                    new QueryResult(userQuery, orderedDocumentIds, k, searchMetrics, relevanceScores.getFrogs())
                                );

                            } catch (Exception ex) {
                                LOGGER.error(
                                    "Unable to get relevance scores for judgments {} and user query {}.",
                                    judgmentsId,
                                    userQuery,
                                    ex
                                );
                            }

                        }

                        @Override
                        public void onFailure(Exception ex) {
                            LOGGER.error("Unable to search using query: {}", searchSourceBuilder.toString(), ex);
                        }
                    });

                }

            }

            // Calculate the search metrics for the entire query set given the individual query set metrics.
            // Sum up the metrics for each query per metric type.
            final int querySetSize = queryResults.size();
            final Map<String, Double> sumOfMetrics = new HashMap<>();
            for (final QueryResult queryResult : queryResults) {
                for (final SearchMetric searchMetric : queryResult.getSearchMetrics()) {
                    // LOGGER.info("Summing: {} - {}", searchMetric.getName(), searchMetric.getValue());
                    sumOfMetrics.merge(searchMetric.getName(), searchMetric.getValue(), Double::sum);
                }
            }

            // Now divide by the number of queries.
            final Map<String, Double> querySetMetrics = new HashMap<>();
            for (final String metric : sumOfMetrics.keySet()) {
                // LOGGER.info("Dividing by the query set size: {} / {}", sumOfMetrics.get(metric), querySetSize);
                querySetMetrics.put(metric, sumOfMetrics.get(metric) / querySetSize);
            }

            final String querySetRunId = UUID.randomUUID().toString();
            final QuerySetRunResult querySetRunResult = new QuerySetRunResult(querySetRunId, querySetId, queryResults, querySetMetrics);

            LOGGER.info("Query set run complete: {}", querySetRunId);

            return querySetRunResult;

        } catch (Exception ex) {
            throw new RuntimeException("Unable to run query set.", ex);
        }

    }

    @Override
    public void save(final QuerySetRunResult result) throws Exception {

        // Now, index the metrics as expected by the dashboards.

        // See https://github.com/o19s/opensearch-search-quality-evaluation/blob/main/opensearch-dashboard-prototyping/METRICS_SCHEMA.md
        // See https://github.com/o19s/opensearch-search-quality-evaluation/blob/main/opensearch-dashboard-prototyping/sample_data.ndjson

        final IndicesExistsRequest indicesExistsRequest = new IndicesExistsRequest(DASHBOARD_METRICS_INDEX_NAME);

        client.admin().indices().exists(indicesExistsRequest, new ActionListener<>() {

            @Override
            public void onResponse(IndicesExistsResponse indicesExistsResponse) {

                if (!indicesExistsResponse.isExists()) {

                    // Create the index.
                    // TODO: Read this mapping from a resource file instead.
                    final String mapping = "{\n"
                        + "              \"properties\": {\n"
                        + "                \"datetime\": { \"type\": \"date\", \"format\": \"strict_date_time\" },\n"
                        + "                \"search_config\": { \"type\": \"keyword\" },\n"
                        + "                \"query_set_id\": { \"type\": \"keyword\" },\n"
                        + "                \"query\": { \"type\": \"keyword\" },\n"
                        + "                \"metric\": { \"type\": \"keyword\" },\n"
                        + "                \"value\": { \"type\": \"double\" },\n"
                        + "                \"application\": { \"type\": \"keyword\" },\n"
                        + "                \"evaluation_id\": { \"type\": \"keyword\" },\n"
                        + "                \"frogs_percent\": { \"type\": \"double\" }\n"
                        + "              }\n"
                        + "          }";

                    // Create the judgments index.
                    final CreateIndexRequest createIndexRequest = new CreateIndexRequest(DASHBOARD_METRICS_INDEX_NAME).mapping(mapping);

                    client.admin().indices().create(createIndexRequest, new ActionListener<>() {

                        @Override
                        public void onResponse(CreateIndexResponse createIndexResponse) {
                            LOGGER.info("{} index created.", DASHBOARD_METRICS_INDEX_NAME);
                        }

                        @Override
                        public void onFailure(Exception ex) {
                            LOGGER.error("Unable to create the {} index.", DASHBOARD_METRICS_INDEX_NAME, ex);
                        }

                    });

                }

            }

            @Override
            public void onFailure(Exception ex) {
                LOGGER.error("Unable to determine if {} index exists.", DASHBOARD_METRICS_INDEX_NAME, ex);
            }

        });

        final BulkRequest bulkRequest = new BulkRequest();
        final String timestamp = TimeUtils.getTimestamp();

        for (final QueryResult queryResult : result.getQueryResults()) {

            for (final SearchMetric searchMetric : queryResult.getSearchMetrics()) {

                // TODO: Make sure all of these items have values.
                final Map<String, Object> metrics = new HashMap<>();
                metrics.put("datetime", timestamp);
                metrics.put("search_config", "research_1");
                metrics.put("query_set_id", result.getQuerySetId());
                metrics.put("query", queryResult.getQuery());
                metrics.put("metric", searchMetric.getName());
                metrics.put("value", searchMetric.getValue());
                metrics.put("application", "sample_data");
                metrics.put("evaluation_id", result.getRunId());
                metrics.put("frogs_percent", queryResult.getFrogs());

                // TODO: This is using the index name from the sample data.
                bulkRequest.add(new IndexRequest("sqe_metrics_sample_data").source(metrics));

            }

        }

        client.bulk(bulkRequest, new ActionListener<>() {

            @Override
            public void onResponse(BulkResponse bulkItemResponses) {
                LOGGER.info("Successfully indexed {} metrics.", bulkItemResponses.getItems().length);
            }

            @Override
            public void onFailure(Exception ex) {
                LOGGER.error("Unable to bulk index metrics.", ex);
            }

        });

    }

}

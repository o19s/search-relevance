/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.runners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensearch.searchrelevance.plugin.metrics.SearchMetric;
import org.opensearch.searchrelevance.plugin.utils.TimeUtils;

/**
 * The results of a query set run.
 */
public class QuerySetRunResult {

    private final String runId;
    private final String querySetId;
    private final List<QueryResult> queryResults;
    private final Map<String, Double> metrics;
    private final String timestamp;

    /**
     * Creates a new query set run result. A random UUID is generated as the run ID.
     * @param runId A unique identifier for this query set run.
     * @param querySetId A unique identifier for the query set.
     * @param queryResults A collection of {@link QueryResult} that contains the queries and search results.
     * @param metrics A map of metric name to value.
     */
    public QuerySetRunResult(
        final String runId,
        final String querySetId,
        final List<QueryResult> queryResults,
        final Map<String, Double> metrics
    ) {
        this.runId = runId;
        this.querySetId = querySetId;
        this.queryResults = queryResults;
        this.metrics = metrics;
        this.timestamp = TimeUtils.getTimestamp();
    }

    /**
     * Get the run's ID.
     * @return The run's ID.
     */
    public String getRunId() {
        return runId;
    }

    /**
     * Gets the query set ID.
     * @return The query set ID.
     */
    public String getQuerySetId() {
        return querySetId;
    }

    /**
     * Gets the search metrics.
     * @return The search metrics.
     */
    public Map<String, Double> getSearchMetrics() {
        return metrics;
    }

    /**
     * Gets the results of the query set run.
     * @return A collection of {@link QueryResult results}.
     */
    public Collection<QueryResult> getQueryResults() {
        return queryResults;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public Collection<Map<String, Object>> getQueryResultsAsMap() {

        final Collection<Map<String, Object>> qs = new ArrayList<>();

        for (final QueryResult queryResult : queryResults) {

            final Map<String, Object> q = new HashMap<>();

            q.put("query", queryResult.getQuery());
            q.put("document_ids", queryResult.getOrderedDocumentIds());
            q.put("frogs", queryResult.getFrogs());

            // Calculate and add each metric to the map.
            for (final SearchMetric searchMetric : queryResult.getSearchMetrics()) {
                q.put(searchMetric.getName(), searchMetric.calculate());
            }

            qs.add(q);

        }

        return qs;

    }

}

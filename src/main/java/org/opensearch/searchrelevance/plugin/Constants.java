/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.searchrelevance.plugin;

public class Constants {

    /**
     * The name of the UBI index containing the queries. This should not be changed.
     */
    public static final String UBI_QUERIES_INDEX_NAME = "ubi_queries";

    /**
     * The name of the UBI index containing the events. This should not be changed.
     */
    public static final String UBI_EVENTS_INDEX_NAME = "ubi_events";

    /**
     * The name of the index to store the scheduled jobs to create implicit judgments.
     */
    public static final String SCHEDULED_JOBS_INDEX_NAME = "search_quality_eval_scheduled_jobs";

    /**
     * The name of the index to store the completed jobs to create implicit judgments.
     */
    public static final String COMPLETED_JOBS_INDEX_NAME = "search_quality_eval_completed_jobs";

    /**
     * The name of the index that stores the query sets.
     */
    public static final String QUERY_SETS_INDEX_NAME = "search_quality_eval_query_sets";

    /**
     * The name of the index that stores the metrics for the dashboard.
     */
    public static final String DASHBOARD_METRICS_INDEX_NAME = "sqe_metrics_sample_data";

    /**
     * The name of the index that stores the implicit judgments.
     */
    public static final String JUDGMENTS_INDEX_NAME = "judgments";

    /**
     * The placeholder in the query that gets replaced by the query term when running a query set.
     */
    public static final String QUERY_PLACEHOLDER = "#$query##";

    private Constants() {

    }

}

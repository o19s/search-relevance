/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin;

public class Constants {

    /**
     * The placeholder in the query that gets replaced by the query term when running a query set.
     */
    public static final String QUERY_PLACEHOLDER = "#$query##";

    /**
     * The name of the UBI index containing the queries. This should not be changed.
     */
    public static final String UBI_QUERIES_INDEX_NAME = "ubi_queries";

    /**
     * The name of the UBI index containing the events. This should not be changed.
     */
    public static final String UBI_EVENTS_INDEX_NAME = "ubi_events";

    /**
     * The name of the COEC rank-aggregated click through index.
     */
    public static final String COEC_RANK_AGGREGATED_CTR_INDEX_NAME = "srw_coec_rank_aggregated_ctr";

    /**
     * The COEC rank-aggregated index mapping.
     */
    public final static String COEC_RANK_AGGREGATED_CTR_INDEX_MAPPING = """
        {
                      "properties": {
                        "position": { "type": "keyword" },
                        "ctr": { "type": "keyword" }
                      }
                  }""";

    /**
     * The name of the COEC clickthrough index.
     */
    public static final String COEC_CTR_INDEX_NAME = "srw_coec_ctr";

    /**
     * The COEC clickthrough index mapping.
     */
    public final static String COEC_CTR_INDEX_MAPPING = """
        {
                      "properties": {
                        "user_query": { "type": "keyword" },
                        "clicks": { "type": "keyword" },
                        "events": { "type": "keyword" },
                        "ctr": { "type": "keyword" },
                        "object_id": { "type": "keyword" }
                      }
                  }""";

    /**
     * The name of the index that stores the judgments.
     */
    public static final String JUDGMENTS_INDEX_NAME = "srw_judgments";

    /**
     * The judgments index mapping.
     */
    public final static String JUDGMENTS_INDEX_MAPPING = """
        {
                      "properties": {
                        "timestamp": { "type": "date", "format": "strict_date_time" },
                        "judgment_set_id": { "type": "keyword" },
                        "judgment_set_type": { "type": "keyword" },
                        "judgment_set_generator": { "type": "keyword" },
                        "judgment_set_name": { "type": "keyword" },
                        "judgment_set_description": { "type": "keyword" },
                        "judgment_set_parameters": { "type": "object" },
                        "user_query": { "type": "keyword" },
                        "query_id": { "type": "keyword" },
                        "document": { "type": "keyword" },
                        "judgment": { "type": "float" }
                      }
                  }""";

    /**
     * The name of the index that stores the query sets.
     */
    public final static String QUERY_SETS_INDEX_NAME = "srw_query_sets";

    /**
     * The query sets index mapping.
     */
    public final static String QUERY_SETS_INDEX_MAPPING = """
        {
                      "properties": {
                        "timestamp": { "type": "date", "format": "strict_date_time" },
                        "description": { "type": "text" },
                        "id": { "type": "keyword" },
                        "name": { "type": "keyword" },
                        "query_set_queries": { "type": "object" },
                        "sampling": { "type": "keyword" }
                      }
                  }""";

    /**
     * THe name of the index that stores the results from each query in a query set run.
     */
    public static final String QUERY_RESULTS_INDEX_NAME = "srw_query_results";

    /**
     * The query sets index mapping.
     */
    public final static String QUERY_RESULTS_MAPPING = """
        {
                      "properties": {
                        "id": { "type": "keyword" },
                        "timestamp": { "type": "date", "format": "strict_date_time" },
                        "query_set_id": { "type": "keyword" },
                        "user_query": { "type": "keyword" },
                        "result_set": { "type": "keyword" },
                        "number_of_results": { "type": "integer" },
                        "evaluation_id": { "type": "keyword" }
                      }
                  }""";

    /**
     * The name of the index that stores the metrics for the dashboard.
     */
    public final static String METRICS_INDEX_NAME = "srw_metrics";

    /**
     * The query results index mapping.
     */
    public static final String METRICS_INDEX_MAPPING = """
        {
                      "properties": {
                        "timestamp": { "type": "date", "format": "strict_date_time" },
                        "search_config": { "type": "keyword" },
                        "query_set_id": { "type": "keyword" },
                        "user_query": { "type": "keyword" },
                        "metric": { "type": "keyword" },
                        "value": { "type": "double" },
                        "application": { "type": "keyword" },
                        "evaluation_id": { "type": "keyword" },
                        "frogs_percent": { "type": "double" }
                      }
                  }""";

    private Constants() {

    }

}

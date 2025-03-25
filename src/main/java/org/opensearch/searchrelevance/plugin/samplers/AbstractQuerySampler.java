/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.samplers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.core.action.ActionListener;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.judgments.opensearch.OpenSearchHelper;
import org.opensearch.searchrelevance.plugin.utils.TimeUtils;

/**
 * An interface for sampling UBI queries.
 */
public abstract class AbstractQuerySampler {

    private static final Logger LOGGER = LogManager.getLogger(AbstractQuerySampler.class);

    protected final OpenSearchHelper openSearchHelper;

    public AbstractQuerySampler(final OpenSearchHelper openSearchHelper) {
        this.openSearchHelper = openSearchHelper;
    }

    /**
     * Gets the name of the sampler.
     * @return The name of the sampler.
     */
    public abstract String getName();

    /**
     * Samples the queries and inserts the query set into an index.
     * @return A query set ID.
     */
    public abstract String sample() throws Exception;

    /**
     * Index the query set.
     */
    protected String indexQuerySet(
        final String querySetId,
        final String name,
        final String description,
        final String sampling,
        final Map<String, Long> queries
    ) {

        LOGGER.info("Indexing {} queries for query set {}", queries.size(), name);

        final Collection<Map<String, Long>> querySetQueries = new ArrayList<>();

        // Convert the queries map to an object.
        for (final String query : queries.keySet()) {

            // Map of the query itself to the frequency of the query.
            final Map<String, Long> querySetQuery = new HashMap<>();
            querySetQuery.put(query, queries.get(query));

            querySetQueries.add(querySetQuery);

        }

        final Map<String, Object> querySet = new HashMap<>();
        querySet.put("name", name);
        querySet.put("description", description);
        querySet.put("sampling", sampling);
        querySet.put("queries", querySetQueries);
        querySet.put("timestamp", TimeUtils.getTimestamp());

        openSearchHelper.createIndexIfNotExists(Constants.QUERY_SETS_INDEX_NAME, Constants.QUERY_SETS_INDEX_MAPPING);

        final IndexRequest indexRequest = new IndexRequest().index(Constants.QUERY_SETS_INDEX_NAME)
            .id(querySetId)
            .source(querySet)
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

        openSearchHelper.getClient().index(indexRequest, new ActionListener<>() {

            @Override
            public void onResponse(IndexResponse indexResponse) {
                LOGGER.info("Indexed query set {} having name {}", querySetId, name);
            }

            @Override
            public void onFailure(Exception ex) {
                LOGGER.error("Unable to index query set {}", querySetId, ex);
            }
        });

        return querySetId;

    }

}

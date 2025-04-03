/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.engines;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.searchrelevance.plugin.model.ClickthroughRate;
import org.opensearch.searchrelevance.plugin.model.GetQuerySetsRequest;
import org.opensearch.searchrelevance.plugin.model.GetSearchConfigurationsRequest;
import org.opensearch.searchrelevance.plugin.model.Judgment;
import org.opensearch.searchrelevance.plugin.model.SearchConfiguration;
import org.opensearch.searchrelevance.plugin.model.ubi.query.UbiQuery;

public interface SearchEngine {

    String getUserQuery(final String queryId) throws Exception;

    /**
     * Gets the query object for a given query ID.
     * @param queryId The query ID.
     * @return A {@link UbiQuery} object for the given query ID.
     * @throws Exception Thrown if the query cannot be retrieved.
     */
    UbiQuery getQueryFromQueryId(final String queryId) throws Exception;

    Collection<String> getQueryIdsHavingUserQuery(final String userQuery) throws Exception;

    long getCountOfQueriesForUserQueryHavingResultInRankR(final String userQuery, final String objectId, final int rank) throws Exception;

    /**
     * Index the rank-aggregated clickthrough values.
     * @param rankAggregatedClickThrough A map of position to clickthrough values.
     */
    void indexRankAggregatedClickthrough(final Map<Integer, Double> rankAggregatedClickThrough);

    /**
     * Index the clickthrough rates.
     * @param clickthroughRates A map of query IDs to a collection of {@link ClickthroughRate} objects.
     * @throws IOException Thrown when there is a problem accessing OpenSearch.
     */
    void indexClickthroughRates(final Map<String, Set<ClickthroughRate>> clickthroughRates) throws Exception;

    void getQuerySets(final GetQuerySetsRequest getQuerySetsRequest, final ActionListener<SearchResponse> listener);

    /**
     * Deletes a query set.
     * @param querySetId The id of the query set to delete.
     * @param listener The listener to call when the deletion is complete.
     */
    void deleteQuerySet(final String querySetId, final ActionListener<DeleteResponse> listener);

    /**
     * Index the judgments.
     * @param judgments A collection of {@link Judgment judgments}.
     * @throws IOException Thrown when there is a problem accessing OpenSearch.
     * @return The ID of the indexed judgments.
     */
    String indexJudgments(final Collection<Judgment> judgments) throws Exception;

    /**
     * Delete a judgment list.
     * @param judgmentId The ID of the judgment list to delete.
     * @param listener The listener to call when the deletion is complete.
     */
    void deleteJudgment(final String judgmentId, final ActionListener<DeleteResponse> listener);

    void createIndexIfNotExists(final String indexName, final String indexMapping);

    long getUserQueryCount(final String userQuery);

    void indexSearchConfiguration(
        final String searchConfigurationId,
        final SearchConfiguration searchConfiguration,
        ActionListener<IndexResponse> listener
    );

    void deleteSearchConfiguration(final String searchConfigurationId, final ActionListener<DeleteResponse> listener);

    void getSearchConfigurations(
        final GetSearchConfigurationsRequest getSearchConfigurationsRequest,
        final ActionListener<SearchResponse> listener
    );

}

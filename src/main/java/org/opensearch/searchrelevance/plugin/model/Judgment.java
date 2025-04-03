/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.model;

import java.util.HashMap;
import java.util.Map;

import org.opensearch.searchrelevance.plugin.utils.MathUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A judgment of a search result's quality for a given query.
 */
public class Judgment {

    private final String queryId;
    private final String userQuery;
    private final String documentId;
    private final double judgment;

    /**
     * Creates a new judgment.
     * @param queryId The query ID for the judgment.
     * @param userQuery The user query for the judgment.
     * @param documentId The document in the jdugment.
     * @param judgment The judgment value.
     */
    public Judgment(final String queryId, final String userQuery, final String documentId, final double judgment) {
        this.queryId = queryId;
        this.userQuery = userQuery;
        this.documentId = documentId;
        this.judgment = judgment;
    }

    public String toJudgmentString() {
        return queryId + ", " + userQuery + ", " + documentId + ", " + MathUtils.round(judgment);
    }

    @JsonIgnore
    public Map<String, Object> getJudgmentAsMap() {

        final Map<String, Object> judgmentMap = new HashMap<>();
        judgmentMap.put("query_id", queryId);
        judgmentMap.put("user_query", userQuery);
        judgmentMap.put("document_id", documentId);
        judgmentMap.put("judgment", judgment);

        return judgmentMap;

    }

    @Override
    public String toString() {
        return "query_id: " + queryId + ", query: " + userQuery + ", document: " + documentId + ", judgment: " + MathUtils.round(judgment);
    }

    /**
     * Gets the judgment's query ID.
     * @return The judgment's query ID.
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Gets the judgment's query.
     * @return The judgment's query.
     */
    public String getUserQuery() {
        return userQuery;
    }

    /**
     * Gets the judgment's document.
     * @return The judgment's document.
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Gets the judgment's value.
     * @return The judgment's value.
     */
    public double getJudgment() {
        return judgment;
    }

}

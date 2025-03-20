/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.judgments.model.ubi.query;

import java.util.Map;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a UBI query.
 */
public class UbiQuery {

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("query_id")
    private String queryId;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("user_query")
    private String userQuery;

    @SerializedName("query")
    private String query;

    @SerializedName("query_attributes")
    private Map<String, String> queryAttributes;

    @SerializedName("query_response")
    private QueryResponse queryResponse;

    /**
     * Creates a new UBI query object.
     */
    public UbiQuery() {

    }

    /**
     * Gets the timestamp for the query.
     * @return The timestamp for the query.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for the query.
     * @param timestamp The timestamp for the query.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Gets the query ID.
     * @return The query ID.
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Sets the query ID.
     * @param queryId The query ID.
     */
    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    /**
     * Sets the client ID.
     * @param clientId The client ID.
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Gets the client ID.
     * @return The client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the user query.
     * @return The user query.
     */
    public String getUserQuery() {
        return userQuery;
    }

    /**
     * Sets the user query.
     * @param userQuery The user query.
     */
    public void setUserQuery(String userQuery) {
        this.userQuery = userQuery;
    }

    /**
     * Gets the query.
     * @return The query.
     */
    public String getQuery() {
        return query;
    }

    /**
     * Sets the query.
     * @param query The query.
     */
    public void setQuery(String query) {
        this.query = query;
    }

    /**
     * Sets the query attributes.
     * @return The query attributes.
     */
    public Map<String, String> getQueryAttributes() {
        return queryAttributes;
    }

    /**
     * Sets the query attributes.
     * @param queryAttributes The query attributes.
     */
    public void setQueryAttributes(Map<String, String> queryAttributes) {
        this.queryAttributes = queryAttributes;
    }

    /**
     * Gets the query responses.
     * @return The query responses.
     */
    public QueryResponse getQueryResponse() {
        return queryResponse;
    }

    /**
     * Sets the query responses.
     * @param queryResponse The query responses.
     */
    public void setQueryResponse(QueryResponse queryResponse) {
        this.queryResponse = queryResponse;
    }

}

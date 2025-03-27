/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.judgments.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchConfiguration {

    private String id;

    @JsonProperty("search_configuration_name")
    private String searchConfigurationName;

    @JsonProperty("query_body")
    private String queryBody;

    @JsonProperty("timestamp")
    private String timestamp;

    public SearchConfiguration() {

    }

    public SearchConfiguration(final String id, final String searchConfigurationName, final String queryBody, final String timestamp) {
        this.id = id;
        this.searchConfigurationName = searchConfigurationName;
        this.queryBody = queryBody;
        this.timestamp = timestamp;
    }

    public String getSearchConfigurationName() {
        return searchConfigurationName;
    }

    public void setSearchConfigurationName(String searchConfigurationName) {
        this.searchConfigurationName = searchConfigurationName;
    }

    public String getQueryBody() {
        return queryBody;
    }

    public void setQueryBody(String queryBody) {
        this.queryBody = queryBody;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}

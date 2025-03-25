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

    @JsonProperty("search_configuration_name")
    private String searchConfigurationName;

    @JsonProperty("query_body")
    private String queryBody;

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

}

/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.model;

import java.util.List;
import java.util.Map;

public class QuerySet {

    private String id;
    private String timestamp;
    private String description;
    private String name;
    private String sampling;
    private List<Map<String, Integer>> querySetQueries;

    public QuerySet(
        final String id,
        final String timestamp,
        final String description,
        final String name,
        final String sampling,
        final List<Map<String, Integer>> querySetQueries
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.description = description;
        this.name = name;
        this.sampling = sampling;
        this.querySetQueries = querySetQueries;
    }

    public QuerySet() {

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSampling() {
        return sampling;
    }

    public void setSampling(String sampling) {
        this.sampling = sampling;
    }

    public List<Map<String, Integer>> getQuerySetQueries() {
        return querySetQueries;
    }

    public void setQuerySetQueries(List<Map<String, Integer>> querySetQueries) {
        this.querySetQueries = querySetQueries;
    }

}

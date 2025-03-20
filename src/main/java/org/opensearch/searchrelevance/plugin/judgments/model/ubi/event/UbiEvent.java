/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.judgments.model.ubi.event;

import com.google.gson.annotations.SerializedName;

/**
 * Creates a representation of a UBI event.
 */
public class UbiEvent {

    @SerializedName("action_name")
    private String actionName;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("query_id")
    private String queryId;

    @SerializedName("event_attributes")
    private EventAttributes eventAttributes;

    /**
     * Creates a new representation of an UBI event.
     */
    public UbiEvent() {

    }

    @Override
    public String toString() {
        return actionName + ", " + clientId + ", " + queryId + ", " + eventAttributes.getObject().toString() + ", " + eventAttributes.getPosition().getOrdinal();
    }

    /**
     * Gets the name of the action.
     * @return The name of the action.
     */
    public String getActionName() {
        return actionName;
    }

    /**
     * Gets the client ID.
     * @return The client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Gets the query ID.
     * @return The query ID.
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Gets the event attributes.
     * @return The {@link EventAttributes}.
     */
    public EventAttributes getEventAttributes() {
        return eventAttributes;
    }

    /**
     * Sets the event attributes.
     * @param eventAttributes The {@link EventAttributes}.
     */
    public void setEventAttributes(EventAttributes eventAttributes) {
        this.eventAttributes = eventAttributes;
    }

}

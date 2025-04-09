/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.rest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.opensearch.client.Request;
import org.opensearch.client.Response;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.test.rest.OpenSearchRestTestCase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for RestPutQuerySetAction
 */
public class RestPutQuerySetActionIT extends OpenSearchRestTestCase {
    private static final String PUT_QUERY_SET_ENDPOINT = "/_plugins/search_relevance/query_sets";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void testPutQuerySetSuccess() throws Exception {
        String name = "test_name";
        String description = "test_description";
        String querySetQueries = "test_string";
        String requestBody = createRequestBody(name, description, querySetQueries);

        Response response = makeRequest("PUT", PUT_QUERY_SET_ENDPOINT, requestBody);
        assertEquals(RestStatus.OK.getStatus(), response.getStatusLine().getStatusCode());
    }

    private String createRequestBody(String name, String description, String querySetQueries) throws JsonProcessingException {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("name", name);
        requestMap.put("description", description);
        requestMap.put("sampling", "manual");
        requestMap.put("querySetQueries", querySetQueries);
        return OBJECT_MAPPER.writeValueAsString(requestMap);
    }

    private Response makeRequest(String method, String endpoint, String body) throws IOException {
        Request request = new Request(method, endpoint);
        if (body != null) {
            request.setJsonEntity(body);
        }
        return client().performRequest(request);
    }
}

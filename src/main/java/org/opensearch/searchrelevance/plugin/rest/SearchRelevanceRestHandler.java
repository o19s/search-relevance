/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.rest;

import static org.opensearch.searchrelevance.plugin.Constants.QUERY_PLACEHOLDER;

import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.delete.DeleteResponse;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.core.action.ActionListener;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.search.SearchHit;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.engines.OpenSearchEngine;
import org.opensearch.searchrelevance.plugin.judgments.clickmodel.coec.CoecClickModel;
import org.opensearch.searchrelevance.plugin.judgments.clickmodel.coec.CoecClickModelParameters;
import org.opensearch.searchrelevance.plugin.model.GetSearchConfigurationsRequest;
import org.opensearch.searchrelevance.plugin.model.SearchConfiguration;
import org.opensearch.searchrelevance.plugin.querysamplers.ProbabilityProportionalToSizeQuerySampler;
import org.opensearch.searchrelevance.plugin.querysamplers.ProbabilityProportionalToSizeQuerySamplerParameters;
import org.opensearch.searchrelevance.plugin.querysamplers.RandomQuerySampler;
import org.opensearch.searchrelevance.plugin.querysamplers.RandomQuerySamplerParameters;
import org.opensearch.searchrelevance.plugin.querysamplers.TopNQuerySampler;
import org.opensearch.searchrelevance.plugin.querysamplers.TopNQuerySamplerParameters;
import org.opensearch.searchrelevance.plugin.querysetrunners.OpenSearchQuerySetRunner;
import org.opensearch.searchrelevance.plugin.querysetrunners.QuerySetRunResult;
import org.opensearch.transport.client.node.NodeClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SearchRelevanceRestHandler extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(SearchRelevanceRestHandler.class);

    private static final String JUDGMENTS_URL = "/_plugins/search_relevance/judgments";
    private static final String QUERYSETS_URL = "/_plugins/search_relevance/query_sets";
    private static final String EXPERIMENTS_URL = "/_plugins/search_relevance/experiments";
    private static final String SEARCH_CONFIGURATIONS_URL = "/_plugins/search_relevance/search_configurations";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getName() {
        return "Search Quality Evaluation Framework";
    }

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.DELETE, JUDGMENTS_URL + "/{id}"),
            new Route(RestRequest.Method.POST, JUDGMENTS_URL),
            new Route(RestRequest.Method.POST, QUERYSETS_URL),
            new Route(RestRequest.Method.DELETE, QUERYSETS_URL + "/{id}"),
            new Route(RestRequest.Method.POST, EXPERIMENTS_URL),
            new Route(RestRequest.Method.POST, SEARCH_CONFIGURATIONS_URL),
            new Route(RestRequest.Method.GET, SEARCH_CONFIGURATIONS_URL),
            new Route(RestRequest.Method.DELETE, SEARCH_CONFIGURATIONS_URL + "/{id}")
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) {

        final OpenSearchEngine openSearchEngine = new OpenSearchEngine(client);

        final String rawPath = request.rawPath();
        LOGGER.info("rawPath = {}", rawPath);

        if (rawPath.startsWith(SEARCH_CONFIGURATIONS_URL)) {

            // Create a new search config.
            if (request.method().equals(RestRequest.Method.POST)) {

                final String requestBody = request.content().utf8ToString();

                final SearchConfiguration searchConfiguration = AccessController.doPrivileged(
                    (PrivilegedAction<SearchConfiguration>) () -> {
                        try {
                            return objectMapper.readValue(requestBody, SearchConfiguration.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    }
                );

                openSearchEngine.createIndexIfNotExists(
                    Constants.SEARCH_CONFIGURATIONS_INDEX_NAME,
                    Constants.SEARCH_CONFIGURATIONS_INDEX_MAPPING
                );

                final String searchConfigurationId = UUID.randomUUID().toString();

                return (channel) -> {
                    openSearchEngine.indexSearchConfiguration(searchConfigurationId, searchConfiguration, new ActionListener<>() {

                        @Override
                        public void onResponse(IndexResponse indexResponse) {
                            channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"id\": \"" + searchConfigurationId + "\"}"));
                        }

                        @Override
                        public void onFailure(Exception e) {
                            channel.sendResponse(
                                new BytesRestResponse(
                                    RestStatus.INTERNAL_SERVER_ERROR,
                                    "{\"error\": \"Unable to create search configuration: " + e.getMessage() + "\"}"
                                )
                            );
                        }
                    });
                };

            } else if (request.method().equals(RestRequest.Method.DELETE)) {

                final String searchConfigurationId = request.param("id");

                return (channel) -> {

                    openSearchEngine.deleteSearchConfiguration(searchConfigurationId, new ActionListener<>() {

                        @Override
                        public void onResponse(DeleteResponse deleteResponse) {
                            if (deleteResponse.getResult() == org.opensearch.action.DocWriteResponse.Result.NOT_FOUND) {
                                channel.sendResponse(new BytesRestResponse(RestStatus.NO_CONTENT, "{\"acknowledged\": \"false\"}"));
                            } else {
                                channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"acknowledged\": \"true\"}"));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            channel.sendResponse(
                                new BytesRestResponse(
                                    RestStatus.INTERNAL_SERVER_ERROR,
                                    "{\"error\": \"Failed to delete: " + e.getMessage() + "\"}"
                                )
                            );
                        }

                    });

                };

            } else if (request.method().equals(RestRequest.Method.GET)) {

                final String requestBody = request.content().utf8ToString();

                final GetSearchConfigurationsRequest getSearchConfigurationsRequest;
                if (!requestBody.isEmpty()) {
                    getSearchConfigurationsRequest = AccessController.doPrivileged(
                        (PrivilegedAction<GetSearchConfigurationsRequest>) () -> {
                            try {
                                return objectMapper.readValue(requestBody, GetSearchConfigurationsRequest.class);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    );
                } else {
                    // None provided so use defaults.
                    getSearchConfigurationsRequest = new GetSearchConfigurationsRequest();
                }

                return (channel) -> {
                    openSearchEngine.getSearchConfigurations(getSearchConfigurationsRequest, new ActionListener<>() {

                        @Override
                        public void onResponse(SearchResponse searchResponse) {

                            final List<SearchConfiguration> searchConfigurations = new ArrayList<>();

                            final ObjectMapper objectMapper = new ObjectMapper();

                            for (final SearchHit hit : searchResponse.getHits().getHits()) {
                                final Map<String, Object> source = hit.getSourceAsMap();
                                searchConfigurations.add(
                                    new SearchConfiguration(
                                        source.get("id").toString(),
                                        source.get("search_configuration_name").toString(),
                                        source.get("query_body").toString(),
                                        source.get("timestamp").toString()
                                    )
                                );
                            }

                            final String jsonResponse = AccessController.doPrivileged((PrivilegedAction<String>) () -> {
                                try {
                                    return objectMapper.writeValueAsString(searchConfigurations);
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }
                            });

                            channel.sendResponse(new BytesRestResponse(RestStatus.OK, jsonResponse));

                        }

                        @Override
                        public void onFailure(Exception ex) {
                            LOGGER.error("Error:", ex);
                            channel.sendResponse(
                                new BytesRestResponse(
                                    RestStatus.INTERNAL_SERVER_ERROR,
                                    "{\"error\": \"Unable to get search configurations: " + ex.getMessage() + "\"}"
                                )
                            );
                        }
                    });
                };

            } else {
                // Invalid HTTP method for this endpoint.
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, "{\"error\": \"" + request.method() + " is not allowed.\"}")
                );
            }

        } else if (rawPath.startsWith(QUERYSETS_URL)) {

            // Creating a new query set by sampling the UBI queries.
            if (request.method().equals(RestRequest.Method.POST)) {

                // TODO: Perhaps these parameters are better passed in the body instead of as query params.
                final String name = request.param("name");
                final String description = request.param("description");
                final String sampling = request.param("sampling", "pptss");
                final int querySetSize = Integer.parseInt(request.param("query_set_size", "1000"));

                // Create a query set using PPTSS sampling.
                if (ProbabilityProportionalToSizeQuerySampler.NAME.equalsIgnoreCase(sampling)) {

                    LOGGER.info("Creating query set using PPTSS");

                    final ProbabilityProportionalToSizeQuerySamplerParameters parameters =
                        new ProbabilityProportionalToSizeQuerySamplerParameters(name, description, sampling, querySetSize);
                    final ProbabilityProportionalToSizeQuerySampler sampler = new ProbabilityProportionalToSizeQuerySampler(
                        openSearchEngine,
                        client,
                        parameters
                    );

                    try {

                        // Sample and index the queries.
                        final String querySetId = sampler.sample();

                        return restChannel -> restChannel.sendResponse(
                            new BytesRestResponse(RestStatus.OK, "{\"query_set\": \"" + querySetId + "\"}")
                        );

                    } catch (Exception ex) {
                        return restChannel -> restChannel.sendResponse(
                            new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"" + ex.getMessage() + "\"}")
                        );
                    }

                } else if (RandomQuerySampler.NAME.equalsIgnoreCase(sampling)) {

                    LOGGER.info("Creating query set using random sampling.");

                    final RandomQuerySamplerParameters parameters = new RandomQuerySamplerParameters(
                        name,
                        description,
                        sampling,
                        querySetSize
                    );

                    final RandomQuerySampler sampler = new RandomQuerySampler(openSearchEngine, parameters);

                    try {

                        // Sample and index the queries.
                        final String querySetId = sampler.sample();

                        return restChannel -> restChannel.sendResponse(
                            new BytesRestResponse(RestStatus.OK, "{\"query_set\": \"" + querySetId + "\"}")
                        );

                    } catch (Exception ex) {
                        return restChannel -> restChannel.sendResponse(
                            new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"" + ex.getMessage() + "\"}")
                        );
                    }

                } else if (TopNQuerySampler.NAME.equalsIgnoreCase(sampling)) {

                    LOGGER.info("Creating query set using top-N sampling.");

                    final TopNQuerySamplerParameters parameters = new TopNQuerySamplerParameters(name, description, sampling, querySetSize);

                    final TopNQuerySampler sampler = new TopNQuerySampler(openSearchEngine, parameters);

                    try {

                        // Sample and index the queries.
                        final String querySetId = sampler.sample();

                        return restChannel -> restChannel.sendResponse(
                            new BytesRestResponse(RestStatus.OK, "{\"query_set\": \"" + querySetId + "\"}")
                        );

                    } catch (Exception ex) {
                        return restChannel -> restChannel.sendResponse(
                            new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, "{\"error\": \"" + ex.getMessage() + "\"}")
                        );
                    }

                } else {
                    // An Invalid sampling method was provided in the request.
                    return restChannel -> restChannel.sendResponse(
                        new BytesRestResponse(RestStatus.BAD_REQUEST, "{\"error\": \"Invalid sampling method: " + sampling + "\"}")
                    );
                }

            } else if (request.method().equals(RestRequest.Method.DELETE)) {

                final String querySetId = request.param("id");

                return (channel) -> {

                    openSearchEngine.deleteQuerySet(querySetId, new ActionListener<>() {

                        @Override
                        public void onResponse(DeleteResponse deleteResponse) {
                            if (deleteResponse.getResult() == org.opensearch.action.DocWriteResponse.Result.NOT_FOUND) {
                                channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND, "{\"acknowledged\": \"false\"}"));
                            } else {
                                channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"acknowledged\": \"true\"}"));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            channel.sendResponse(
                                new BytesRestResponse(
                                    RestStatus.INTERNAL_SERVER_ERROR,
                                    "{\"error\": \"Failed to delete query set: " + e.getMessage() + "\"}"
                                )
                            );
                        }

                    });

                };

            } else {
                // Invalid HTTP method for this endpoint.
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, "{\"error\": \"" + request.method() + " is not allowed.\"}")
                );
            }

        } else if (EXPERIMENTS_URL.equalsIgnoreCase(request.path())) {

            // TODO: Read these from the POST body - issue #40 - https://github.com/o19s/search-relevance/issues/40
            final String querySetId = request.param("id");
            final String judgmentsId = request.param("judgments_id");
            final String index = request.param("index");
            final String searchPipeline = request.param("search_pipeline", null);
            final String idField = request.param("id_field", "_id");
            final int k = Integer.parseInt(request.param("k", "10"));
            final double threshold = Double.parseDouble(request.param("threshold", "1.0"));
            final String application = request.param("application", null);
            final String searchConfiguration = request.param("search_configuration", null);

            if (querySetId == null
                || querySetId.isEmpty()
                || judgmentsId == null
                || judgmentsId.isEmpty()
                || index == null
                || index.isEmpty()) {
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.BAD_REQUEST, "{\"error\": \"Missing required parameters.\"}")
                );
            }

            if (k < 1) {
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.BAD_REQUEST, "{\"error\": \"k must be a positive integer.\"}")
                );
            }

            if (!request.hasContent()) {
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.BAD_REQUEST, "{\"error\": \"Missing query in body.\"}")
                );
            }

            // Get the query JSON from the content.
            final String query = new String(BytesReference.toBytes(request.content()), Charset.defaultCharset());

            // Validate the query has a QUERY_PLACEHOLDER.
            if (!query.contains(QUERY_PLACEHOLDER)) {
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.BAD_REQUEST, "{\"error\": \"Missing query placeholder in query.\"}")
                );
            }

            try {

                final OpenSearchQuerySetRunner openSearchQuerySetRunner = new OpenSearchQuerySetRunner(openSearchEngine);
                final QuerySetRunResult querySetRunResult = openSearchQuerySetRunner.run(
                    querySetId,
                    judgmentsId,
                    index,
                    searchPipeline,
                    idField,
                    query,
                    k,
                    threshold,
                    application,
                    searchConfiguration
                );
                openSearchQuerySetRunner.save(querySetRunResult);

            } catch (Exception ex) {
                LOGGER.error("Unable to run query set. Verify query set and judgments exist.", ex);
                return restChannel -> restChannel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
            }

            return restChannel -> restChannel.sendResponse(
                new BytesRestResponse(RestStatus.OK, "{\"message\": \"Run initiated for query set " + querySetId + "\"}")
            );

        } else if (rawPath.startsWith(JUDGMENTS_URL)) {

            if (request.method().equals(RestRequest.Method.POST)) {

                // final long startTime = System.currentTimeMillis();
                final String clickModel = request.param("click_model", "coec");
                final int maxRank = Integer.parseInt(request.param("max_rank", "20"));

                if (CoecClickModel.CLICK_MODEL_NAME.equalsIgnoreCase(clickModel)) {

                    final CoecClickModelParameters coecClickModelParameters = new CoecClickModelParameters(maxRank);
                    final CoecClickModel coecClickModel = new CoecClickModel(client, coecClickModelParameters);

                    final String judgmentsId;

                    // TODO: Run this in a separate thread.
                    try {

                        openSearchEngine.createIndexIfNotExists(Constants.JUDGMENTS_INDEX_NAME, Constants.JUDGMENTS_INDEX_MAPPING);

                        judgmentsId = coecClickModel.calculateJudgments();

                        // judgmentsId will be null if no judgments were created (and indexed).
                        if (judgmentsId == null) {
                            // TODO: Is Bad Request the appropriate error? Perhaps Conflict is more appropriate?
                            return restChannel -> restChannel.sendResponse(
                                new BytesRestResponse(
                                    RestStatus.BAD_REQUEST,
                                    "{\"error\": \"No judgments were created. Check the queries and events data.\"}"
                                )
                            );
                        }

                        // final long elapsedTime = System.currentTimeMillis() - startTime;
                        //
                        // final Map<String, Object> job = new HashMap<>();
                        // job.put("name", "manual_generation");
                        // job.put("click_model", clickModel);
                        // job.put("started", startTime);
                        // job.put("duration", elapsedTime);
                        // job.put("invocation", "on_demand");
                        // job.put("judgments_id", judgmentsId);
                        // job.put("max_rank", maxRank);
                        //
                        // final String jobId = UUID.randomUUID().toString();
                        //
                        // final IndexRequest indexRequest = new IndexRequest()
                        // .index(SearchQualityEvaluationPlugin.COMPLETED_JOBS_INDEX_NAME)
                        // .id(jobId)
                        // .source(job)
                        // .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
                        //
                        // client.index(indexRequest, new ActionListener<>() {
                        // @Override
                        // public void onResponse(final IndexResponse indexResponse) {
                        // LOGGER.debug("Click model job completed successfully: {}", jobId);
                        // }
                        //
                        // @Override
                        // public void onFailure(final Exception ex) {
                        // LOGGER.error("Unable to run job with ID {}", jobId, ex);
                        // throw new RuntimeException("Unable to run job", ex);
                        // }
                        // });

                    } catch (Exception ex) {
                        throw new RuntimeException("Unable to generate judgments.", ex);
                    }

                    return restChannel -> restChannel.sendResponse(
                        new BytesRestResponse(RestStatus.OK, "{\"judgments_id\": \"" + judgmentsId + "\"}")
                    );

                } else {
                    return restChannel -> restChannel.sendResponse(
                        new BytesRestResponse(RestStatus.BAD_REQUEST, "{\"error\": \"Invalid click model.\"}")
                    );
                }

            } else if (request.method().equals(RestRequest.Method.DELETE)) {

                final String judgmentId = request.param("id");

                return (channel) -> {

                    openSearchEngine.deleteJudgment(judgmentId, new ActionListener<>() {

                        @Override
                        public void onResponse(DeleteResponse deleteResponse) {
                            if (deleteResponse.getResult() == org.opensearch.action.DocWriteResponse.Result.NOT_FOUND) {
                                channel.sendResponse(new BytesRestResponse(RestStatus.NOT_FOUND, "{\"acknowledged\": \"false\"}"));
                            } else {
                                channel.sendResponse(new BytesRestResponse(RestStatus.OK, "{\"acknowledged\": \"true\"}"));
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            channel.sendResponse(
                                new BytesRestResponse(
                                    RestStatus.INTERNAL_SERVER_ERROR,
                                    "{\"error\": \"Failed to delete judgment: " + e.getMessage() + "\"}"
                                )
                            );
                        }

                    });

                };

            } else {
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, "{\"error\": \"" + request.method() + " is not allowed.\"}")
                );
            }

        } else {
            return restChannel -> restChannel.sendResponse(
                new BytesRestResponse(RestStatus.NOT_FOUND, "{\"error\": \"" + request.path() + " was not found.\"}")
            );
        }

    }

}

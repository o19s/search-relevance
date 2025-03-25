/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.rest;

import static org.opensearch.searchrelevance.plugin.Constants.QUERY_PLACEHOLDER;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.core.common.bytes.BytesReference;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.searchrelevance.plugin.Constants;
import org.opensearch.searchrelevance.plugin.judgments.clickmodel.coec.CoecClickModel;
import org.opensearch.searchrelevance.plugin.judgments.clickmodel.coec.CoecClickModelParameters;
import org.opensearch.searchrelevance.plugin.judgments.opensearch.OpenSearchHelper;
import org.opensearch.searchrelevance.plugin.runners.OpenSearchQuerySetRunner;
import org.opensearch.searchrelevance.plugin.runners.QuerySetRunResult;
import org.opensearch.searchrelevance.plugin.samplers.ProbabilityProportionalToSizeQuerySampler;
import org.opensearch.searchrelevance.plugin.samplers.ProbabilityProportionalToSizeQuerySamplerParameters;
import org.opensearch.searchrelevance.plugin.samplers.RandomQuerySampler;
import org.opensearch.searchrelevance.plugin.samplers.RandomQuerySamplerParameters;
import org.opensearch.searchrelevance.plugin.samplers.TopNQuerySampler;
import org.opensearch.searchrelevance.plugin.samplers.TopNQuerySamplerParameters;
import org.opensearch.transport.client.node.NodeClient;

public class SearchRelevanceRestHandler extends BaseRestHandler {

    private static final Logger LOGGER = LogManager.getLogger(SearchRelevanceRestHandler.class);

    private static final String IMPLICIT_JUDGMENTS_URL = "/_plugins/search_relevance/judgments";
    private static final String QUERYSET_MANAGEMENT_URL = "/_plugins/search_relevance/queryset";
    private static final String QUERYSET_RUN_URL = "/_plugins/search_relevance/experiments";

    @Override
    public String getName() {
        return "Search Quality Evaluation Framework";
    }

    @Override
    public List<Route> routes() {
        return List.of(
            new Route(RestRequest.Method.POST, IMPLICIT_JUDGMENTS_URL),
            new Route(RestRequest.Method.POST, QUERYSET_MANAGEMENT_URL),
            new Route(RestRequest.Method.POST, QUERYSET_RUN_URL)
        );
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {

        final OpenSearchHelper openSearchHelper = new OpenSearchHelper(client);

        // Handle managing query sets.
        if (QUERYSET_MANAGEMENT_URL.equalsIgnoreCase(request.path())) {

            // Creating a new query set by sampling the UBI queries.
            if (request.method().equals(RestRequest.Method.POST)) {

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
                        openSearchHelper,
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

                    final RandomQuerySampler sampler = new RandomQuerySampler(openSearchHelper, parameters);

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

                    final TopNQuerySampler sampler = new TopNQuerySampler(openSearchHelper, parameters);

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

            } else {
                // Invalid HTTP method for this endpoint.
                return restChannel -> restChannel.sendResponse(
                    new BytesRestResponse(RestStatus.METHOD_NOT_ALLOWED, "{\"error\": \"" + request.method() + " is not allowed.\"}")
                );
            }

            // Handle running query sets.
        } else if (QUERYSET_RUN_URL.equalsIgnoreCase(request.path())) {

            final String querySetId = request.param("id");
            final String judgmentsId = request.param("judgments_id");
            final String index = request.param("index");
            final String searchPipeline = request.param("search_pipeline", null);
            final String idField = request.param("id_field", "_id");
            final int k = Integer.parseInt(request.param("k", "10"));
            final double threshold = Double.parseDouble(request.param("threshold", "1.0"));

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

                final OpenSearchQuerySetRunner openSearchQuerySetRunner = new OpenSearchQuerySetRunner(client, openSearchHelper);
                final QuerySetRunResult querySetRunResult = openSearchQuerySetRunner.run(
                    querySetId,
                    judgmentsId,
                    index,
                    searchPipeline,
                    idField,
                    query,
                    k,
                    threshold
                );
                openSearchQuerySetRunner.save(querySetRunResult);

            } catch (Exception ex) {
                LOGGER.error("Unable to run query set. Verify query set and judgments exist.", ex);
                return restChannel -> restChannel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, ex.getMessage()));
            }

            return restChannel -> restChannel.sendResponse(
                new BytesRestResponse(RestStatus.OK, "{\"message\": \"Run initiated for query set " + querySetId + "\"}")
            );

            // Handle the on-demand creation of implicit judgments.
        } else if (IMPLICIT_JUDGMENTS_URL.equalsIgnoreCase(request.path())) {

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

                        openSearchHelper.createIndexIfNotExists(Constants.JUDGMENTS_INDEX_NAME, Constants.JUDGMENTS_INDEX_MAPPING);

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

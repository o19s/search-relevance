/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.samplers;

public class AllQueriesQueryQuerySamplerParameters extends AbstractQuerySamplerParameters {

    public static final String SAMPLER = "all";

    public AllQueriesQueryQuerySamplerParameters(
        final String name,
        final String description,
        final String sampling,
        final int querySetSize
    ) {
        super(SAMPLER, name, description, sampling, querySetSize);
    }

}

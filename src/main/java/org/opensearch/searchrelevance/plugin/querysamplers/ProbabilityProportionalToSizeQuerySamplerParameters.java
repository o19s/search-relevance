/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.querysamplers;

public class ProbabilityProportionalToSizeQuerySamplerParameters extends AbstractQuerySamplerParameters {

    public static final String SAMPLER = "pptss";

    public ProbabilityProportionalToSizeQuerySamplerParameters(
        final String name,
        final String description,
        final String sampling,
        final int querySetSize
    ) {
        super(SAMPLER, name, description, sampling, querySetSize);
    }

}

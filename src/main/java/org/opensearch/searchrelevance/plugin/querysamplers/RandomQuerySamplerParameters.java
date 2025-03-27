/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.querysamplers;

public class RandomQuerySamplerParameters extends AbstractQuerySamplerParameters {

    public RandomQuerySamplerParameters(String name, String description, String sampling, int querySetSize) {
        super(RandomQuerySampler.NAME, name, description, sampling, querySetSize);
    }

}

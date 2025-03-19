/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Subclass of {@link SearchMetric} that calculates Normalized Discounted Cumulative Gain @ k.
 */
public class NdcgSearchMetric extends DcgSearchMetric {

    /**
     * Creates new NDCG metrics.
     * @param k The <code>k</code> value.
     * @param relevanceScores A list of relevancy scores.
     */
    public NdcgSearchMetric(final int k, final List<Double> relevanceScores) {
        super(k, relevanceScores);
    }

    @Override
    public String getName() {
        return "ndcg_at_" + k;
    }

    @Override
    public double calculate() {

        double dcg = super.calculate();

        if(dcg == 0) {

            // The ndcg is 0. No need to continue.
            return 0;

        } else {

            final List<Double> idealRelevanceScores = new ArrayList<>(relevanceScores);
            idealRelevanceScores.sort(Collections.reverseOrder());

            double idcg = super.calculateDcg(idealRelevanceScores);

            if(idcg == 0) {
                return 0;
            } else {
                return dcg / idcg;
            }

        }

    }

}

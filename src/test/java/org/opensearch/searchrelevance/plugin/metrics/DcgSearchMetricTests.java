/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.searchrelevance.plugin.metrics;

import java.util.List;

import org.opensearch.test.OpenSearchTestCase;

public class DcgSearchMetricTests extends OpenSearchTestCase {

    public void testCalculate() {

        final int k = 10;
        final List<Double> relevanceScores = List.of(1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 0.0);

        final DcgSearchMetric dcgSearchMetric = new DcgSearchMetric(k, relevanceScores);
        final double dcg = dcgSearchMetric.calculate();

        assertEquals(13.864412483585935, dcg, 0.0);

    }

    public void testCalculateAllZeros() {

        final int k = 10;
        final List<Double> relevanceScores = List.of(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

        final DcgSearchMetric dcgSearchMetric = new DcgSearchMetric(k, relevanceScores);
        final double dcg = dcgSearchMetric.calculate();

        assertEquals(0.0, dcg, 0.0);

    }

}

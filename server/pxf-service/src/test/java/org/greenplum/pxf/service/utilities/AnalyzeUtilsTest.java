package org.greenplum.pxf.service.utilities;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.greenplum.pxf.api.model.Fragment;
import org.greenplum.pxf.api.model.RequestContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AnalyzeUtilsTest {

    @Test
    public void generateSamplingBitSet() {
        runGenerateSamplingBitSetTest(10, 5, new int[]{0, 3, 4, 6, 9});

        runGenerateSamplingBitSetTest(9, 8, new int[]{0, 2, 3, 4, 5, 6, 7, 8});

        runGenerateSamplingBitSetTest(10, 10, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});

        runGenerateSamplingBitSetTest(8, 0, new int[]{});

        runGenerateSamplingBitSetTest(8, 3, new int[]{0, 3, 6});
    }

    @Test
    @Disabled("flakey test due to java heap space (memory err)")
    public void generateSamplingBitSetBig() {
        BitSet result = AnalyzeUtils.generateSamplingBitSet(1000000, 990000);
        assertEquals(result.cardinality(), 990000);
        assertTrue(result.length() < 1000000);

        result = AnalyzeUtils.generateSamplingBitSet(1000000000, 5000000);
        assertEquals(result.cardinality(), 5000000);
        assertTrue(result.length() < 1000000000);
    }

    @Test
    public void getSampleFragments() {
        // fragments less than threshold
        runGetSampleFragmentsTest(4, 100, 4, new int[]{0, 1, 2, 3});

        // fragments over threshold
        runGetSampleFragmentsTest(4, 2, 2, new int[]{0, 3});
        runGetSampleFragmentsTest(10, 2, 2, new int[]{0, 6});
        runGetSampleFragmentsTest(10, 3, 3, new int[]{0, 4, 8});
        runGetSampleFragmentsTest(10, 9, 9, new int[]{0, 1, 2, 4, 5, 6, 7, 8, 9});
        runGetSampleFragmentsTest(15, 10, 10, new int[]{0, 2, 3, 4, 6, 7, 8, 10, 12, 14});
        runGetSampleFragmentsTest(1000, 10, 10,
                new int[]{0, 101, 202, 303, 404, 505, 606, 707, 808, 909});
        runGetSampleFragmentsTest(100, 65, 65,
                new int[]{0, 1, 2, 4, 5, 6, 8, 9, 10,       /* 9 elements */
                        12, 13, 14, 16, 17, 18,           /* 6 elements */
                        20, 21, 22, 24, 25, 26, 28, 29,   /* 8 elements */
                        30, 32, 33, 34, 36, 37, 38,       /* 7 elements */
                        40, 41, 42, 44, 45, 46, 48, 49,   /* 8 elements */
                        50, 52, 53, 54, 56, 57, 58,       /* 7 elements */
                        60, 62, 64, 66, 68,               /* 5 elements */
                        70, 72, 74, 76, 78,               /* 5 elements */
                        80, 82, 84, 86, 88,               /* 5 elements */
                        90, 92, 94, 96, 98                /* 5 elements */
                });
        /* => 65 elements */
        // threshold illegal and ignored
        runGetSampleFragmentsTest(10, 0, 10, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9});
    }

    private void runGenerateSamplingBitSetTest(int poolSize, int sampleSize, int[] expectedIndexes) {
        BitSet expected = new BitSet();
        for (int i : expectedIndexes) {
            expected.set(i);
        }
        BitSet result = AnalyzeUtils.generateSamplingBitSet(poolSize, sampleSize);

        assertEquals(expected, result);
    }

    private void runGetSampleFragmentsTest(int inputSize, int maxFragments, int expectedSize, int[] expectedIndexes) {
        RequestContext mockContext = mock(RequestContext.class);
        when(mockContext.getStatsMaxFragments()).thenReturn(maxFragments);

        List<Fragment> fragments = new ArrayList<>();

        for (int i = 0; i < inputSize; i++) {
            fragments.add(prepareFragment(i));
        }
        assertEquals(inputSize, fragments.size());

        List<Fragment> result = AnalyzeUtils.getSampleFragments(fragments, mockContext);

        List<Fragment> expected = new ArrayList<>();

        for (int i : expectedIndexes) {
            expected.add(prepareFragment(i));
        }

        assertEquals(expectedSize, result.size(), "verify number of returned fragments");

        for (int i = 0; i < expectedSize; i++) {
            assertEquals(expected.get(i).getIndex(), result.get(i).getIndex(), "compare fragment #" + i);
        }
    }

    private Fragment prepareFragment(int i) {
        Fragment fragment = new Fragment("fragment" + i, null, null);
        fragment.setIndex(i);
        return fragment;
    }
}

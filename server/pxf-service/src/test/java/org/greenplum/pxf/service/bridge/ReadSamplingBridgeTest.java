package org.greenplum.pxf.service.bridge;

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


import org.junit.jupiter.api.Disabled;

// TODO remove Disabled
@Disabled
public class ReadSamplingBridgeTest {

    /**
     * Writable test object to test ReadSamplingBridge. The object receives a
     * string and returns it in its toString function.
     *
    public static class WritableTest implements Writable {

        private final String data;

        public WritableTest(String data) {
            this.data = data;
        }

        @Override
        public void write(DataOutput out) throws IOException {
            throw new IOException("not implemented");
        }

        @Override
        public void readFields(DataInput in) throws IOException {
            throw new IOException("not implemented");
        }

        @Override
        public String toString() {
            return data;
        }

    }

    private RequestContext context;
    private ReadBridge mockBridge;

    private ReadSamplingBridge readSamplingBridge;
    private int recordsLimit = 0;
    private BitSet samplingBitSet;
    private Writable result;

    @BeforeEach
    public void setUp() throws Exception {

        context = new RequestContext();
        context.setConfiguration(new Configuration());

        mockBridge = mock(ReadBridge.class);

        when(mockBridge.getNext()).thenAnswer(new Answer<Writable>() {
            private int count = 0;

            @Override
            public Writable answer(InvocationOnMock invocation) {
                if (count >= recordsLimit) {
                    return null;
                }
                return new WritableTest("" + (count++));
            }
        });

        samplingBitSet = new BitSet();
        when(
                AnalyzeUtils.generateSamplingBitSet(any(int.class),
                        any(int.class))).thenReturn(samplingBitSet);
    }

    @Test
    public void getNextRecord100Percent() throws Exception {

        samplingBitSet.set(0, 100);
        recordsLimit = 100;
        context.setStatsSampleRatio(1.0F);

        // readSamplingBridge = new ReadSamplingBridge(new BridgeOutputBuilder(context), mockResolverFactory, context);

        result = readSamplingBridge.getNext();
        assertEquals("0", result.toString());

        result = readSamplingBridge.getNext();
        assertEquals("1", result.toString());

        when(mockBridge.getNext()).thenReturn(null);

        result = readSamplingBridge.getNext();
        assertNull(result);
    }

    @Test
    public void getNextRecord100Records10Percent() throws Exception {

        // set 10 bits from 5 to 14.
        samplingBitSet.set(5, 15);
        recordsLimit = 100;
        context.setStatsSampleRatio(0.1F);

        // readSamplingBridge = new ReadSamplingBridge(mockAccessorFactory, mockResolverFactory);

        for (int i = 0; i < 10; i++) {
            result = readSamplingBridge.getNext();
            assertEquals("" + (i + 5), result.toString());
        }

        result = readSamplingBridge.getNext();
        assertNull(result);
    }

    @Test
    public void getNextRecord100Records90Percent() throws Exception {
        int expected;

        // set the first odd numbers until 20, then all numbers until 100
        // total: 90.
        samplingBitSet.set(0, 100);
        for (int i = 0; i < 10; i++) {
            samplingBitSet.flip(i * 2);
        }
        recordsLimit = 100;
        context.setStatsSampleRatio(0.9F);

        // readSamplingBridge = new ReadSamplingBridge(mockAccessorFactory, mockResolverFactory);

        for (int i = 0; i < 90; i++) {
            result = readSamplingBridge.getNext();
            if (i < 10) {
                expected = (i * 2) + 1;
            } else {
                expected = i + 10;
            }
            assertEquals("" + expected, result.toString());
        }
        result = readSamplingBridge.getNext();
        assertNull(result);
    }

    @Test
    public void getNextRecord350Records50Percent() throws Exception {

        // set bits 0, 40-79 (40) and 90-98 (9)
        // total 50.
        samplingBitSet.set(0);
        samplingBitSet.set(40, 80);
        samplingBitSet.set(90, 99);
        recordsLimit = 350;
        context.setStatsSampleRatio(0.5F);

        // readSamplingBridge = new ReadSamplingBridge(mockAccessorFactory, mockResolverFactory);

        /*
         * expecting to have: 50 (out of first 100) 50 (out of second 100) 50
         * (out of third 100) 11 (out of last 50) --- 161 records
         *
        for (int i = 0; i < 161; i++) {
            result = readSamplingBridge.getNext();
            assertNotNull(result);
            if (i % 50 == 0) {
                assertEquals("" + (i * 2), result.toString());
            }
        }
        result = readSamplingBridge.getNext();
        assertNull(result);
    }

    @Test
    public void getNextRecord100000Records30Sample() throws Exception {
        int expected;

        // ratio = 0.0003
        float ratio = (float) (30.0 / 100000.0);

        // set 3 records in every 10000.
        samplingBitSet.set(99);
        samplingBitSet.set(999);
        samplingBitSet.set(9999);
        recordsLimit = 100000;
        context.setStatsSampleRatio(ratio);

        // readSamplingBridge = new ReadSamplingBridge(mockAccessorFactory, mockResolverFactory);

        for (int i = 0; i < 30; i++) {
            result = readSamplingBridge.getNext();
            assertNotNull(result);
            int residue = i % 3;
            int div = i / 3;
            if (residue == 0) {
                expected = 99 + (div * 10000);
            } else if (residue == 1) {
                expected = 999 + (div * 10000);
            } else {
                expected = 9999 + (div * 10000);
            }
            assertEquals("" + expected, result.toString());
        }
        result = readSamplingBridge.getNext();
        assertNull(result);
    }
    */
}

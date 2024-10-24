package org.greenplum.pxf.api.model;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;

/**
 * FragmentStats holds statistics for a given path.
 */
@Getter
public class FragmentStats {
    /**
     * Default fragment size. Assuming a fragment is equivalent to a block in
     * HDFS, we guess a full fragment size is 64MB.
     */
    public static final long DEFAULT_FRAGMENT_SIZE = 67108864L;
    private long fragmentsNumber;
    private SizeAndUnit firstFragmentSize;
    private SizeAndUnit totalSize;
    public enum SizeUnit {
        /**
         * Byte
         */
        B,
        /**
         * KB
         */
        KB,
        /**
         * MB
         */
        MB,
        /**
         * GB
         */
        GB,
        /**
         * TB
         */
        TB
    }

    /**
     * Container for size and unit
     */
    @Getter
    public static class SizeAndUnit {
        long size;
        SizeUnit unit;

        /**
         * Default constructor.
         */
        public SizeAndUnit() {
            this.size = 0;
            this.unit = SizeUnit.B;
        }

        @Override
        public String toString() {
            return size + "" + unit;
        }
    }

    /**
     * Constructs an FragmentStats.
     *
     * @param fragmentsNumber number of fragments
     * @param firstFragmentSize first fragment size (in bytes)
     * @param totalSize total size (in bytes)
     */
    public FragmentStats(long fragmentsNumber, long firstFragmentSize,
                         long totalSize) {
        this.setFragmentsNumber(fragmentsNumber);
        this.setFirstFragmentSize(firstFragmentSize);
        this.setTotalSize(totalSize);
    }

    /**
     * Given a {@link FragmentStats}, serialize it in JSON to be used as the
     * result string for GPDB. An example result is as follows:
     * <code>{"PXFFragmentsStats":{"fragmentsNumber":3,"firstFragmentSize":{"size"=67108864,"unit":"B"},"totalSize":{"size"=200000000,"unit"="B"}}}</code>
     *
     * @param stats the data to be serialized
     * @return the result in json format
     * @throws IOException if converting to JSON format failed
     */
    public static String dataToJSON(FragmentStats stats) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // mapper serializes all members of the class by default
        return "{\"PXFFragmentsStats\":" + mapper.writeValueAsString(stats)
                + "}";
    }

    /**
     * Given a stats structure, convert it to be readable. Intended for
     * debugging purposes only.
     *
     * @param stats the data to be stringify
     * @param datapath the data path part of the original URI (e.g., table name,
     *            *.csv, etc.)
     * @return the stringified data
     */
    public static String dataToString(FragmentStats stats, String datapath) {
        return "Statistics information for \"" + datapath + "\" "
                + " Number of Fragments: " + stats.fragmentsNumber
                + ", first Fragment size: " + stats.firstFragmentSize
                + ", total size: " + stats.totalSize;
    }

    private void setFragmentsNumber(long fragmentsNumber) {
        this.fragmentsNumber = fragmentsNumber;
    }

    private void setFirstFragmentSize(long firstFragmentSize) {
        this.firstFragmentSize = setSizeAndUnit(firstFragmentSize);
    }

    private void setTotalSize(long totalSize) {
        this.totalSize = setSizeAndUnit(totalSize);
    }

    private SizeAndUnit setSizeAndUnit(long originalSize) {
        final long THRESHOLD = Integer.MAX_VALUE / 2;
        int orderOfMagnitude = 0;
        SizeAndUnit sizeAndUnit = new SizeAndUnit();
        sizeAndUnit.size = originalSize;

        while (sizeAndUnit.size > THRESHOLD) {
            sizeAndUnit.size /= 1024;
            orderOfMagnitude++;
        }

        sizeAndUnit.unit = getSizeUnit(orderOfMagnitude);
        return sizeAndUnit;
    }

    private SizeUnit getSizeUnit(int orderOfMagnitude) {
        SizeUnit unit;
        switch (orderOfMagnitude) {
            case 0:
                unit = SizeUnit.B;
                break;
            case 1:
                unit = SizeUnit.KB;
                break;
            case 2:
                unit = SizeUnit.MB;
                break;
            case 3:
                unit = SizeUnit.GB;
                break;
            case 4:
                unit = SizeUnit.TB;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported order of magnitude "
                                + orderOfMagnitude
                                + ". Size's order of magnitue can be a value between 0(Bytes) and 4(TB)");
        }
        return unit;
    }
}

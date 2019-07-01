package org.greenplum.pxf.plugins.jdbc.partitioning;

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

import org.greenplum.pxf.plugins.jdbc.utils.DbProduct;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class IntPartitionTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private DbProduct dbProduct = null;

    private final String COL_RAW = "col";
    private final String QUOTE = "\"";
    private final String COL = QUOTE + COL_RAW + QUOTE;

    @Test
    public void testNormal() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{0L, 1L}, new boolean[]{true, true});
        String constraint = partition.toSqlConstraint(QUOTE, dbProduct);

        assertEquals(
            COL + " >= 0 AND " + COL + " <= 1",
            constraint
        );
    }

    @Test
    public void testRightBounded() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{null, 0L}, new boolean[]{false, false});
        String constraint = partition.toSqlConstraint(QUOTE, dbProduct);

        assertEquals(
            COL + " < 0",
            constraint
        );
    }

    @Test
    public void testLeftBounded() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{0L, null}, new boolean[]{false, false});
        String constraint = partition.toSqlConstraint(QUOTE, dbProduct);

        assertEquals(
            COL + " > 0",
            constraint
        );
    }

    @Test
    public void testRightBoundedInclusive() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{null, 0L}, new boolean[]{false, true});
        String constraint = partition.toSqlConstraint(QUOTE, dbProduct);

        assertEquals(
            COL + " <= 0",
            constraint
        );
    }

    @Test
    public void testLeftBoundedInclusive() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{0L, null}, new boolean[]{true, false});
        String constraint = partition.toSqlConstraint(QUOTE, dbProduct);

        assertEquals(
            COL + " >= 0",
            constraint
        );
    }

    @Test
    public void testEqualBoundaries() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{0L, 0L}, new boolean[]{true, true});
        String constraint = partition.toSqlConstraint(QUOTE, dbProduct);

        assertEquals(
            COL + " = 0",
            constraint
        );
    }

    @Test
    public void testInvalidBothBoundariesNull() throws Exception {
        thrown.expect(RuntimeException.class);

        new IntPartition(COL_RAW, null, null);
    }

    @Test
    public void testInvalidColumnNull() throws Exception {
        thrown.expect(RuntimeException.class);

        new IntPartition(null, new Long[]{0L, 1L}, new boolean[]{true, true});
    }

    @Test
    public void testInvalidNullQuoteString() throws Exception {
        IntPartition partition = new IntPartition(COL_RAW, new Long[]{0L, 1L}, new boolean[]{true, true});

        thrown.expect(RuntimeException.class);

        partition.toSqlConstraint(null, dbProduct);
    }
}

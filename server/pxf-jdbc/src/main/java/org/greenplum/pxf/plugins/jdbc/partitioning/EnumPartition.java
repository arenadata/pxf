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
import org.greenplum.pxf.plugins.jdbc.partitioning.PartitionType;

import java.io.Serializable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EnumPartition implements JdbcFragmentMetadata, Serializable {
    private static final long serialVersionUID = 1L;

    private final String column;
    private final String value;
    private final String[] excluded;

    /**
     * Construct an EnumPartition with given column and constraint
     * @param column
     * @param value
     */
    public EnumPartition(String column, String value) {
        assert column != null;
        assert value != null;

        this.column = column;
        this.value = value;
        excluded = null;
    }

    /**
     * Construct an EnumPartition with given column and a special (exclusion) constraint.
     * The partition created by this constructor contains all values that differ from the given ones.
     * @param column
     * @param excluded array of values this partition must NOT include
     */
    public EnumPartition(String column, String[] excluded) {
        assert column != null;
        assert excluded != null && excluded.length > 0;

        this.column = column;
        value = null;
        this.excluded = excluded;
    }

    @Override
    public String getColumn() {
        return column;
    }

    @Override
    public PartitionType getType() {
        return PartitionType.ENUM;
    }

    @Override
    public String toSqlConstraint(String quoteString, DbProduct dbProduct) {
        assert quoteString != null;
        assert (value != null && excluded == null) || (value == null && excluded != null);

        StringBuilder sb = new StringBuilder();

        String columnQuoted = quoteString + column + quoteString;

        if (excluded == null) {
            sb.append(columnQuoted).append(" = '").append(value).append("'");
        }
        else {
            // We use multiple inequality as this is the widest supported method to perform this operation
            sb.append(Stream.of(excluded)
                .map(excludedValue -> columnQuoted + " <> '" + excludedValue + "'")
                .collect(Collectors.joining(" AND "))
            );
        }

        return sb.toString();
    }

    public String getValue() {
        return value;
    }

    public String[] getExcluded() {
        return excluded;
    }
}
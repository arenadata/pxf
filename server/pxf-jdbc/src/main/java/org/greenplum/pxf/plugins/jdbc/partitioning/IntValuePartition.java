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

import lombok.Getter;
import org.greenplum.pxf.plugins.jdbc.utils.DbProduct;


@Getter
public class IntValuePartition extends BaseValuePartition implements IntPartition {

    private final long value;

    /**
     * @param column the partition column
     * @param value  value to base constraint on
     */
    public IntValuePartition(String column, long value) {
        super(column);
        this.value = value;
    }

    @Override
    public String toSqlConstraint(String quoteString, DbProduct dbProduct) {
        return generateConstraint(
                getQuotedColumn(quoteString),
                String.valueOf(value)
        );
    }

    @Override
    public Long getStart() {
        return value;
    }

    @Override
    public Long getEnd() {
        return value;
    }
}

package org.greenplum.pxf.plugins.hbase;

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


import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.greenplum.pxf.api.OneField;
import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.error.BadRecordException;
import org.greenplum.pxf.api.error.UnsupportedTypeException;
import org.greenplum.pxf.api.io.DataType;
import org.greenplum.pxf.api.model.BasePlugin;
import org.greenplum.pxf.api.model.Resolver;
import org.greenplum.pxf.plugins.hbase.utilities.HBaseColumnDescriptor;
import org.greenplum.pxf.plugins.hbase.utilities.HBaseTupleDescription;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

/**
 * Record resolver for HBase.
 * <p>
 * The class is responsible to convert rows from HBase scans (returned as {@link Result} objects)
 * into a List of {@link OneField} objects.
 * That also includes the conversion process of each HBase column's value into its GPDB assigned type.
 * <p>
 * Currently, the class assumes all HBase values are stored as String object Bytes encoded.
 */
public class HBaseResolver extends BasePlugin implements Resolver {
    private HBaseTupleDescription tupleDescription;

    @Override
    public void afterPropertiesSet() {
        tupleDescription = new HBaseTupleDescription(context);
    }

    /**
     * Splits an HBase {@link Result} object into a list of {@link OneField},
     * based on the table's tuple description.
     * Each field is converted from HBase bytes into its column description type.
     *
     * @return list of fields
     */
    @Override
    public List<OneField> getFields(OneRow onerow) throws Exception {
        Result result = (Result) onerow.getData();
        List<OneField> fields = new LinkedList<>();

        for (int i = 0; i < tupleDescription.columns(); ++i) {
            HBaseColumnDescriptor column = tupleDescription.getColumn(i);
            byte[] value;

            if (column.isKeyColumn()) // if a row column is requested
            {
                value = result.getRow(); // just return the row key
            } else // else, return column value
            {
                value = getColumnValue(result, column);
            }

            OneField oneField = new OneField();
            oneField.type = column.columnTypeCode();
            oneField.val = convertToJavaObject(oneField.type, column.columnTypeName(), value);
            fields.add(oneField);
        }
        return fields;
    }

    /**
     * Constructs and sets the fields of a {@link OneRow}.
     *
     * @param record list of {@link OneField}
     * @return the constructed {@link OneRow}
     */
    @Override
    public OneRow setFields(List<OneField> record) {
        throw new UnsupportedOperationException("HBase resolver does not support write operation");
    }

    /**
     * Converts given byte array value to the matching java object, according to
     * the given type code.
     *
     * @param typeCode ColumnDescriptor type id
     * @param typeName type name. Used for error messages
     * @param val value to be converted
     * @return value converted to matching object type
     * @throws Exception when conversion fails or type code is not supported
     */
    Object convertToJavaObject(int typeCode, String typeName, byte[] val) throws Exception {
        if (val == null) {
            return null;
        }
        try {
            switch (DataType.get(typeCode)) {
                case TEXT:
                case VARCHAR:
                case BPCHAR:
                case NUMERIC:
                    return Bytes.toString(val);

                case INTEGER:
                    return Integer.parseInt(Bytes.toString(val));

                case BIGINT:
                    return Long.parseLong(Bytes.toString(val));

                case SMALLINT:
                    return Short.parseShort(Bytes.toString(val));

                case REAL:
                    return Float.parseFloat(Bytes.toString(val));

                case FLOAT8:
                    return Double.parseDouble(Bytes.toString(val));

                case BYTEA:
                    return val;

                case BOOLEAN:
                    return Boolean.valueOf(Bytes.toString(val));

                case TIMESTAMP:
                    return Timestamp.valueOf(Bytes.toString(val));

                default:
                    throw new UnsupportedTypeException("Unsupported data type " + typeName);
            }
        } catch (NumberFormatException e) {
            throw new BadRecordException("Error converting value '" + Bytes.toString(val) + "' " +
                    "to type " + typeName + ". " +
                    "(original error: " + e.getMessage() + ")");
        }
    }

    /**
     * Returns the value of a column from a Result object.
     *
     * @param result HBase table row
     * @param column HBase column to be retrieved
     * @return HBase column value
     */
    byte[] getColumnValue(Result result, HBaseColumnDescriptor column) {
        // if column does not contain a value, return null
        if (!result.containsColumn(column.columnFamilyBytes(),
                column.qualifierBytes())) {
            return null;
        }

        // else, get the latest version of the requested column
        Cell cell = result.getColumnLatestCell(column.columnFamilyBytes(), column.qualifierBytes());
        return CellUtil.cloneValue(cell);
    }
}

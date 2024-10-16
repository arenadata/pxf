package org.greenplum.pxf.api.examples;

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

import org.greenplum.pxf.api.OneField;
import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.io.DataType;
import org.greenplum.pxf.api.model.InputStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that defines the serialization / deserialization of one record brought from the external input data.
 * <p>
 * Demo implementation of resolver that returns text format
 */
@InputStreamHandler
public class DemoTextResolver extends DemoResolver {

    /**
     * Read the next record
     * The record contains as many fields as defined by the DDL schema.
     *
     * @param row one record
     * @return the first column contains the entire text data
     */
    @Override
    public List<OneField> getFields(OneRow row) {
        List<OneField> output = new LinkedList<>();
        Object data = row.getData();
        output.add(new OneField(DataType.VARCHAR.getOID(), data));
        return output;
    }

    /**
     * Creates a OneRow object from the singleton list.
     *
     * @param record list of {@link OneField}
     * @return the constructed {@link OneRow}
     * @throws Exception if constructing a row from the fields failed
     */
    @Override
    public OneRow setFields(List<OneField> record) throws Exception {
        // text row data is passed as a single field
        if (record == null || record.size() != 1) {
            throw new Exception("Unexpected record format, expected 1 field, found " +
                    (record == null ? 0 : record.size()));
        }
        int readCount;
        byte[] data = new byte[1024];
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataInputStream dis = (DataInputStream) record.get(0).val;
        while ((readCount = dis.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, readCount);
        }
        buffer.flush();
        byte[] bytes= buffer.toByteArray();
        // empty array means the end of input stream, return null to stop iterations
        return bytes.length == 0 ? null : new OneRow(bytes);
    }
}

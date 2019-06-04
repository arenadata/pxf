package org.greenplum.pxf.plugins.tkh;

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
import org.greenplum.pxf.api.model.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.List;

/**
 * JDBC tables resolver
 */
public class TkhResolver extends TkhPlugin implements Resolver {
    private static final Logger LOG = LoggerFactory.getLogger(TkhResolver.class);

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    /**
     * getFields() implementation
     *
     * @param row one row
     * @throws SQLException if the provided {@link OneRow} object is invalid
     */
    @Override
    public List<OneField> getFields(OneRow row) throws SQLException {
        throw new UnsupportedOperationException("SELECT is not supported");
    }

    /**
     * setFields() implementation
     *
     * @param record List of fields
     * @return OneRow with the data field containing a List of fields
     * OneFields are not reordered before being passed to Accessor; at the
     * moment, there is no way to correct the order of the fields if it is not.
     * In practice, the 'record' provided is always ordered the right way.
     * @throws UnsupportedOperationException if field of some type is not supported
     * @throws ParseException                if the record cannot be parsed
     */
    @Override
    public OneRow setFields(List<OneField> record) throws IOException {
        for (OneField oneField : record) {
            baos.write(oneField.val.toString().getBytes(StandardCharsets.UTF_8));
            baos.write('\t');
        }
        baos.write('\n');

        byte[] result = baos.toByteArray();
        baos.reset();

        return new OneRow(result);
    }
}

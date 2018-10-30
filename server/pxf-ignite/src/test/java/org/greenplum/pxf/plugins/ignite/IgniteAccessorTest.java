package org.greenplum.pxf.plugins.ignite;

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

import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.io.DataType;
import org.greenplum.pxf.api.utilities.ColumnDescriptor;
import org.greenplum.pxf.api.utilities.InputData;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Matchers.any;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({IgniteAccessor.class, Ignition.class, ClientConfiguration.class, IgniteClient.class, SqlFieldsQuery.class, FieldsQueryCursor.class})
public class IgniteAccessorTest {
    @Mock
    private InputData inputData;
    private ArrayList<ColumnDescriptor> columns = new ArrayList<>();

    @Mock
    private IgniteClient igniteClient;
    @Mock
    private FieldsQueryCursor<List<?>> cursor;

    @Before
    public void prepareAccessorTest() throws Exception {
        // InputData
        PowerMockito.when(inputData.getDataSource()).thenReturn("TableTest");
        PowerMockito.when(inputData.getUserProperty("HOST")).thenReturn("0.0.0.0");
        PowerMockito.when(inputData.hasFilter()).thenReturn(false);
        columns.add(new ColumnDescriptor("id", DataType.INTEGER.getOID(), 0, "int4", null));
        columns.add(new ColumnDescriptor("name", DataType.TEXT.getOID(), 1, "text", null));
        columns.add(new ColumnDescriptor("birthday", DataType.DATE.getOID(), 2, "date", null));
        PowerMockito.when(inputData.getTupleDescription()).thenReturn(columns);
        PowerMockito.when(inputData.getColumn(0)).thenReturn(columns.get(0));
        PowerMockito.when(inputData.getColumn(1)).thenReturn(columns.get(1));
        PowerMockito.when(inputData.getColumn(2)).thenReturn(columns.get(2));

        // Fill result and mock it
        List<Object> result_row = new ArrayList<>();
        result_row.add(Object.class.cast(new Integer(1)));
        result_row.add(Object.class.cast(new String("Mocked name")));
        result_row.add(Object.class.cast(new Date(946674000)));
        result.add(result_row);

        result_row = new ArrayList<Object>(result_row);
        result_row.set(0, Object.class.cast(new Integer(2)));
        result.add(new ArrayList<Object>(result_row));

        result_row = new ArrayList<Object>(result_row);
        result_row.set(0, Object.class.cast(new Integer(3)));
        result.add(new ArrayList<Object>(result_row));

        // Ignite Client
        PowerMockito.mockStatic(Ignition.class);
        PowerMockito.when(cursor.getAll()).thenReturn(null);
        Mockito.when(cursor.iterator()).thenReturn(result.iterator());
        PowerMockito.when(igniteClient.query(any(SqlFieldsQuery.class))).thenReturn(cursor);
        PowerMockito.when(Ignition.startClient(any(ClientConfiguration.class))).thenReturn(igniteClient);
    }

    @Test
    public void testReadAccess() throws Exception {
        IgniteAccessor acc = PowerMockito.spy(new IgniteAccessor(inputData));

        // Conduct test
        OneRow rows[] = new OneRow[3];
        acc.openForRead();
        rows[0] = acc.readNextObject();
        rows[1] = acc.readNextObject();
        rows[2] = acc.readNextObject();
        Object rows_4 = acc.readNextObject();
        acc.closeForRead();

        // Asserts
        assertNull(rows_4);
        assertEquals(new Integer(1), Integer.class.cast(List.class.cast(rows[0].getData()).get(0)));
        assertEquals(new String("Mocked name"), String.class.cast(List.class.cast(rows[0].getData()).get(1)));
        assertEquals(new Date(946674000), Date.class.cast(List.class.cast(rows[0].getData()).get(2)));
        assertEquals(new Integer(2), Integer.class.cast(List.class.cast(rows[1].getData()).get(0)));
        assertEquals(new Integer(3), Integer.class.cast(List.class.cast(rows[2].getData()).get(0)));
    }

    private List<List<?>> result = new ArrayList<List<?>>();
}

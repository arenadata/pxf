package org.greenplum.pxf.api.utilities;

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


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class ColumnDescriptorTest {


    @Test
    public void testConstructor() {

        ColumnDescriptor cd = new ColumnDescriptor("c1", 42, 0, "someDataType", new Integer[]{42, 46});

        ColumnDescriptor clonned = new ColumnDescriptor(cd);

        assertEquals(clonned.columnName(), cd.columnName());
        assertEquals(clonned.columnTypeCode(), cd.columnTypeCode());
        assertEquals(clonned.columnIndex(), cd.columnIndex());
        assertEquals(clonned.columnTypeName(), cd.columnTypeName());
        assertArrayEquals(clonned.columnTypeModifiers(), cd.columnTypeModifiers());
        assertEquals(clonned.isKeyColumn(), cd.isKeyColumn());

        //Cloned instance should have reference to different array
        assertFalse(clonned.columnTypeModifiers() == cd.columnTypeModifiers());

        new ColumnDescriptor(null, 0, 0, null, null);
    }

}

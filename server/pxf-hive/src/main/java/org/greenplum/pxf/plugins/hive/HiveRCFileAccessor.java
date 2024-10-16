package org.greenplum.pxf.plugins.hive;

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


import org.apache.hadoop.hive.ql.io.RCFileInputFormat;
import org.apache.hadoop.hive.ql.io.RCFileRecordReader;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.greenplum.pxf.api.filter.Operator;
import org.greenplum.pxf.api.io.DataType;
import org.greenplum.pxf.api.utilities.SerializationService;
import org.greenplum.pxf.api.utilities.SpringContext;
import org.greenplum.pxf.plugins.hive.utilities.HiveUtilities;

import java.io.IOException;
import java.util.EnumSet;

/**
 * Specialization of HiveAccessor for a Hive table that stores only RC files.
 * This class replaces the generic HiveAccessor for a case where a table is stored entirely as RC files.
 * Use together with {@link HiveInputFormatFragmenter}/{@link HiveColumnarSerdeResolver}
 */
public class HiveRCFileAccessor extends HiveAccessor {

    /**
     * Constructs a HiveRCFileAccessor.
     */
    public HiveRCFileAccessor() {
        super(new RCFileInputFormat<>(),
                SpringContext.getBean(HiveUtilities.class),
                SpringContext.getBean(SerializationService.class));
    }

    @Override
    protected Object getReader(JobConf jobConf, InputSplit split) throws IOException {
        return new RCFileRecordReader<>(jobConf, (FileSplit) split);
    }

    @Override
    protected EnumSet<Operator> getSupportedOperatorsForPushdown() {
        return ORC_SUPPORTED_OPERATORS; // RC same as ORC
    }

    @Override
    protected EnumSet<DataType> getSupportedDatatypesForPushdown() {
        return ORC_SUPPORTED_DATATYPES; // RC same as ORC
    }
}

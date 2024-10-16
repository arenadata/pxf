package org.greenplum.pxf.plugins.hdfs.utilities;

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

import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.ByteWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.VIntWritable;
import org.apache.hadoop.io.Writable;
import org.greenplum.pxf.api.OneField;
import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.api.utilities.ColumnDescriptor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Adapter used for adding a recordkey field to the records output
 * {@code List<OneField>}.
 */
public class RecordkeyAdapter {

    private static final Log LOG = LogFactory.getLog(RecordkeyAdapter.class);

    // private Log LOG = LogFactory.getLog(RecordkeyAdapter.class);

    /*
     * We need to transform Record keys to java primitive types. Since the type
     * of the key is the same throughout the file we do the type resolution in
     * the first call (for the first record) and then use a
     * "Java variation on Function pointer" to do the extraction for the rest of
     * the records.
     */
    private interface ValExtractor {
        Object get(Object key);
    }

    private ValExtractor extractor = null;

    private interface ValConverter {
        Writable get(Object key);
    }

    private ValConverter converter = null;

    /**
     * Constructs a RecordkeyAdapter.
     */
    public RecordkeyAdapter() {
    }

    /**
     * Adds the recordkey to the end of the passed in recFields list.
     * <p>
     * This method also verifies cases in which record keys are not supported by
     * the underlying source type, and therefore "illegally" requested.
     *
     * @param recFields existing list of record (non-key) fields and their
     *            values.
     * @param input all input parameters coming from the client request
     * @param onerow a row object which is used here in order to find out if the
     *            given type supports recordkeys or not.
     * @return 0 if record key not needed, or 1 if record key was appended
     * @throws NoSuchFieldException when the given record type does not support
     *             recordkeys
     */
    public int appendRecordkeyField(List<OneField> recFields, RequestContext input,
                                    OneRow onerow) throws NoSuchFieldException {

        /*
         * user did not request the recordkey field in the
         * "create external table" statement
         */
        ColumnDescriptor recordkeyColumn = input.getRecordkeyColumn();
        if (recordkeyColumn == null) {
            return 0;
        }

        /*
         * The recordkey was filled in the fileAccessor during execution of
         * method readNextObject. The current accessor implementations are
         * SequenceFileAccessor, LineBreakAccessor and AvroFileAccessor from
         * HdfsSplittableDataAccessor and QuotedLineBreakAccessor from
         * HdfsAtomicDataAccessor. For SequenceFileAccessor, LineBreakAccessor
         * the recordkey is set, since it is returned by the
         * SequenceFileRecordReader or LineRecordReader(for text file). But Avro
         * files do not have keys, so the AvroRecordReader will not return a key
         * and in this case recordkey will be null. If the user specified a
         * recordkey attribute in the CREATE EXTERNAL TABLE statement and he
         * reads from an AvroFile, we will throw an exception since the Avro
         * file does not have keys In the future, additional implementations of
         * FileAccessors will have to set recordkey during readNextObject().
         * Otherwise it is null by default and we will throw an exception here,
         * that is if we get here... a careful user will not specify recordkey
         * in the CREATE EXTERNAL statement and then we will leave this function
         * one line above.
         */
        Object recordkey = onerow.getKey();
        if (recordkey == null) {
            throw new NoSuchFieldException(
                    "Value for field \"recordkey\" was requested but the "
                            + "queried HDFS resource type does not support key");
        }

        OneField oneField = new OneField();
        oneField.type = recordkeyColumn.columnTypeCode();
        oneField.val = extractVal(recordkey);
        recFields.add(oneField);
        return 1;
    }

    /*
     * Extracts a java primitive type value from the recordkey. If the key is a
     * Writable implementation we extract the value as a Java primitive. If the
     * key is already a Java primitive we returned it as is If it is an unknown
     * type we throw an exception
     */
    private Object extractVal(Object key) {
        if (extractor == null) {
            extractor = InitializeExtractor(key);
        }

        return extractor.get(key);
    }

    /*
     * Initialize the extractor object based on the type of the recordkey
     */
    private ValExtractor InitializeExtractor(Object obj) {
        if (obj instanceof IntWritable) {
            return key -> ((IntWritable) key).get();
        } else if (obj instanceof ByteWritable) {
            return key -> ((ByteWritable) key).get();
        } else if (obj instanceof BooleanWritable) {
            return key -> ((BooleanWritable) key).get();
        } else if (obj instanceof DoubleWritable) {
            return key -> ((DoubleWritable) key).get();
        } else if (obj instanceof FloatWritable) {
            return key -> ((FloatWritable) key).get();
        } else if (obj instanceof LongWritable) {
            return key -> ((LongWritable) key).get();
        } else if (obj instanceof Text) {
            return Object::toString;
        } else if (obj instanceof VIntWritable) {
            return key -> ((VIntWritable) key).get();
        } else {
            return key -> {
                throw new UnsupportedOperationException(
                        "Unsupported recordkey data type "
                                + key.getClass().getName());
            };
        }
    }

    /**
     * Converts given key object to its matching Writable. Supported types:
     * Integer, Byte, Boolean, Double, Float, Long, String. The type is only
     * checked once based on the key, all consequent calls must be of the same
     * type.
     *
     * @param key object to convert
     * @return Writable object matching given key
     */
    public Writable convertKeyValue(Object key) {
        if (converter == null) {
            converter = initializeConverter(key);
            LOG.debug("converter initialized for type " + key.getClass()
                    + " (key value: " + key + ")");
        }

        return converter.get(key);
    }

    private ValConverter initializeConverter(Object obj) {
        if (obj instanceof Integer) {
            return key -> (new IntWritable((Integer) key));
        } else if (obj instanceof Byte) {
            return key -> (new ByteWritable((Byte) key));
        } else if (obj instanceof Boolean) {
            return key -> (new BooleanWritable((Boolean) key));
        } else if (obj instanceof Double) {
            return key -> (new DoubleWritable((Double) key));
        } else if (obj instanceof Float) {
            return key -> (new FloatWritable((Float) key));
        } else if (obj instanceof Long) {
            return key -> (new LongWritable((Long) key));
        } else if (obj instanceof String) {
            return key -> (new Text((String) key));
        } else {
            return key -> {
                throw new UnsupportedOperationException(
                        "Unsupported recordkey data type "
                                + key.getClass().getName());
            };
        }
    }
}

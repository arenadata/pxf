package org.greenplum.pxf.plugins.json;

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

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.LineRecordReader;
import org.apache.hadoop.mapred.RecordReader;
import org.greenplum.pxf.plugins.json.parser.PartitionedJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Multi-line json object reader. JsonRecordReader uses a member name (set by the <b>IDENTIFIER</b> PXF parameter) to
 * determine the encapsulating object to extract and read.
 * <p>
 * JsonRecordReader supports compressed input files as well.
 * <p>
 * As a safe guard set the optional <b>MAXLENGTH</b> parameter to limit the max size of a record.
 */
public class JsonRecordReader implements RecordReader<LongWritable, Text> {

    public static final String RECORD_MEMBER_IDENTIFIER = "json.input.format.record.identifier";
    public static final String RECORD_MAX_LENGTH = "multilinejsonrecordreader.maxlength";
    private static final Logger LOG = LoggerFactory.getLogger(JsonRecordReader.class);
    private final long start;
    private long pos;
    private final long end;
    private final int maxObjectLength;
    private final PartitionedJsonParser parser;
    private LineRecordReader lineRecordReader;
    // position of the underlying lineRecordReader
    private long filePos;
    // line that was read in by the line record reader
    private final Text currentLine;
    private final JobConf conf;
    private final Path file;
    // this is the current line in buffer form
    private StringBuffer currentLineBuffer;
    // index where the JsonRecordReader has read to in the currentLineBuffer
    private int currentLineIndex = Integer.MAX_VALUE;
    private boolean inNextSplit = false;
    // used to store characters that were read before we hit a json begin object marker
    private final StringBuilder readValues = new StringBuilder(MAX_CHARS);
    // max number of characters to read before we update the position
    private static final int MAX_CHARS = 1024;

    private static final char BACKSLASH = '\\';
    private static final char QUOTE = '\"';
    private static final char START_BRACE = '{';
    private static final int EOF = -1;
    private static final int END_OF_SPLIT = -2;
    private static final byte[] NEW_LINE = "\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] CARRIAGERETURN_NEWLINE = "\r\n".getBytes(StandardCharsets.UTF_8);
    private final LongWritable key;

    /**
     * Create new multi-line json object reader.
     *
     * @param conf  Hadoop context
     * @param split HDFS split to start the reading from
     * @throws IOException IOException when reading the file
     */
    public JsonRecordReader(JobConf conf, FileSplit split) throws IOException {

        String jsonMemberName = conf.get(RECORD_MEMBER_IDENTIFIER);
        maxObjectLength = conf.getInt(RECORD_MAX_LENGTH, Integer.MAX_VALUE);

        start = split.getStart();
        end = start + split.getLength();
        file = split.getPath();
        lineRecordReader =  new LineRecordReader(conf, split);
        this.conf = conf;
        parser = new PartitionedJsonParser(jsonMemberName);
        currentLine = new Text();
        // set pos and filePos to lineRecordReader's position. If the split started in the middle of a line,
        // we assume that the previous split has taken care of it, so we just need to be at the same starting
        // point as the lineRecordReader.
        filePos = lineRecordReader.getPos();
        pos = filePos;
        key = lineRecordReader.createKey();
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public boolean next(LongWritable key, Text value) throws IOException {

        while (!inNextSplit) { // split level. if out of split, then return false
            // scan to first start brace object.
            boolean foundBeginObject = scanToNextJsonBeginObject();
            if (!foundBeginObject) {
                // if we've scanned the entire file/split and didn't find anything,
                // then the position of the JsonRecordReader should match the lineRecordReader
                pos = filePos;
                return false;
            }

            // found a start brace so begin a new json object
            parser.startNewJsonObject();

            // read through the file until the object is completed
            int i;
            boolean isObjectComplete = false;
            while (!isObjectComplete && (i = readNextChar()) != EOF) {
                // if we are at the end of the split, then we need to get the next split before we can read the line
                if (i == END_OF_SPLIT) {
                    LOG.debug("JSON object incomplete, continuing into next split to finish");
                    getNextSplit();
                    // continue the while loop to complete the object
                    continue;
                }

                char c = (char) i;
                // object is complete if we found a matching } for either the starting {
                // or for an internal object that has a field with the matching identifier
                isObjectComplete = parser.parse(c);
            }

            // if we have gotten here there are 2 scenarios:
            // - the object is not complete, in which case we hit end of file
            // - the object is complete, in which case, determine if we found the identifier
            if (!isObjectComplete) {
                // if we have gotten here, filePos is at EOF
                pos = filePos;
                return false;
            }

            String json = parser.getCompletedObject();

            // the object is complete so update the position
            pos += json.getBytes(StandardCharsets.UTF_8).length;

            // if we found the identifier
            if (parser.foundObjectWithIdentifier()) {
                // check the char length of the json against the MAXLENGTH parameter
                long jsonLength = json.length();
                if (jsonLength > maxObjectLength) {
                    LOG.warn("Skipped JSON object of size {}", json.length());
                } else {
                    // the key is set to the length of the json object
                    key.set(jsonLength);
                    value.set(json);
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public LongWritable createKey() {
        return new LongWritable();
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public Text createValue() {
        return new Text();
    }

    @Override
    public long getPos() {
        return pos;
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public synchronized void close() throws IOException {
        if (lineRecordReader != null) {
            lineRecordReader.close();
        }
    }

    /*
     * {@inheritDoc}
     */
    @Override
    public float getProgress() {
        if (start == end) {
            return 0.0f;
        } else {
            return Math.min(1.0f, (pos - start) / (float) (end - start));
        }
    }

    /**
     * Reads the next character in the buffer. It will pull the next line as necessary
     *
     * @return the int value of a character
     * @throws IOException if I/O error occurs
     */
    private int readNextChar() throws IOException {
        // if the currentLineBuffer is null, nothing has been read yet, so we need to read the next line
        // if the currentLineIndex is greater than the length,  we are at the end of the buffer, read the next line
        boolean refreshLineBuffer = currentLineBuffer == null || currentLineIndex >= currentLineBuffer.length();
        if (refreshLineBuffer && !getNextLine()) {
            return END_OF_SPLIT;
        }

        int c = currentLineBuffer.charAt(currentLineIndex);
        currentLineIndex++;

        return c;

    }

    /**
     * Read through the characters until we hit starting bracket that indicates the start of a JSON object
     *
     * @return true when an open bracket '{' is found, false otherwise
     * @throws IOException if I/O error occurs
     */
    private boolean scanToNextJsonBeginObject() throws IOException {
        // assumes each line is a valid json line
        // seek until we hit the first begin-object
        boolean inString = false;
        int i;

        // reset the stringbuilder and start counting chars
        readValues.setLength(0);

        // since we have not yet found a starting object, exit if either EOF (-1) or END_OF_SPLIT (-2)
        while ((i = readNextChar()) > EOF) {
            char c = (char) i;

            // while scanning to start of object, we need to make sure we aren't losing our position
            accountForCharacterRead(c);
            // if the current value is a backslash, then ignore the next value as it's an escaped char
            if (c == BACKSLASH) {
                accountForCharacterRead((char) readNextChar());
            } else if (c == QUOTE) {
                inString = !inString;
            } else if (c == START_BRACE && !inString) {
                // the start brace will be accounted for later, so ignore it for now
                pos += readValues.toString().getBytes(StandardCharsets.UTF_8).length - 1;
                return true;
            }
        }
        pos += readValues.toString().getBytes(StandardCharsets.UTF_8).length;
        return false;
    }

    /**
     * Append the given character to the string of characters read
     * <p>
     * If the number of characters read equals MAX_CHARS, then
     * set the global pos and reset the string of characters read to empty
     * @param c the character that was read
     */
    private void accountForCharacterRead(char c) {
        readValues.append(c);
        if (readValues.length() >= MAX_CHARS){
            pos += readValues.toString().getBytes(StandardCharsets.UTF_8).length;
            readValues.setLength(0);
        }
    }

    /**
     * This function allows JsonRecordReader to go into the next split to finish a JSON object.
     * Closes the current LineRecordReader and opens a new one that starts at the end of the current split
     * The end of the new split is set to Long.MAX
     *
     * @throws IOException if I/O error occurs
     */
    private void getNextSplit() throws IOException {
        // close the old lineRecordReader
        lineRecordReader.close();
        // we need to move into the next split, so create one that starts at the end of the current split
        // and goes until the end of the file
        FileSplit nextSplit = new FileSplit(file, end, Long.MAX_VALUE - end, (String[]) null);
        lineRecordReader = new LineRecordReader(conf, nextSplit);
        inNextSplit = true;
    }

    /**
     * Reads the next line of the file in to begin parsing the characters
     *
     * @return true if a line was read, false otherwise. False means that we have reached the end of the split
     * @throws IOException if error occurs internally in underlying LineRecordReader
     */
    private boolean getNextLine() throws IOException {
        currentLine.clear();
        long currentPos = lineRecordReader.getPos();
        // use lineRecordReader which internally will handle splits for us: will return false when the split ends
        boolean didReturnLine = lineRecordReader.next(key, currentLine);
        filePos = lineRecordReader.getPos();
        if (didReturnLine) {
            // lineRecordReader removes the new lines and carriage returns when it does the read
            // we want to track that delta, so we know the proper size of the line that was returned
            long delta = filePos - currentPos - currentLine.getLength();
            // append the removed chars back for proper accounting
            if (delta == 2) {
                currentLine.append(CARRIAGERETURN_NEWLINE, 0, CARRIAGERETURN_NEWLINE.length);
            } else if (delta == 1) {
                currentLine.append(NEW_LINE, 0, NEW_LINE.length);
            } else if (delta > 2) {
                LOG.warn("LineRecordReader removed delta = {} characters while parsing a line in the JSON file at pos {}", delta, filePos);
            }
            currentLineBuffer = new StringBuffer(currentLine.toString());
            currentLineIndex = 0;
        }
        return didReturnLine;
    }
}

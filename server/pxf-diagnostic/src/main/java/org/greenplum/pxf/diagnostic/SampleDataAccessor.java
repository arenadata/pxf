package org.greenplum.pxf.diagnostic;

import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.error.PxfRuntimeException;
import org.greenplum.pxf.api.model.Accessor;
import org.greenplum.pxf.api.model.BasePlugin;

import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Test class for regression tests that generates rows of sample data.
 * The returned data has 4 columns delimited with DELIMITER property value.
 * One million rows will be generated unless a ROWS property is specified.
 * An error can be optionally generated by a specific segment for a random or a specific row.
 * <p>
 * Use the following SQL statement to create a corresponding Greenplum external table:
 * CREATE EXTERNAL TABLE pxf_sample (a text, b integer, c boolean, d numeric)
 * LOCATION ('pxf://dummy?PROFILE=system:sample') FORMAT 'CSV';
 * <p>
 * Additional options that can be set for the table:
 * ROWS - a number of rows that each segment will return
 * ERROR_SEGMENT - a segment id that will throw a "test error" during streaming
 * ERROR_ROW - a row number that will have a "test error" thrown in its place
 * If ERROR_SEGMENT is specified but ERROR_ROW is not, then the segment will generate an error for a random row.
 */
public class SampleDataAccessor extends BasePlugin implements Accessor {

    private static final String UNSUPPORTED_ERR_MESSAGE = "SampleDataAccessor does not support write operation";
    private static final String ROWS_PROPERTY = "ROWS";
    private static final String ERROR_SEGMENT_PROPERTY = "ERROR_SEGMENT";
    private static final String ERROR_ROW_PROPERTY = "ERROR_ROW";

    private static final long DEFAULT_ROWS = 1000000;
    private static final int NO_ERROR_SEGMENT = -10;
    private static final long NO_ERROR_ROW = -1L;

    private int segmentId;
    private String userDelimiter;

    private long row = 0;
    private long numRows = 0;
    private int errorSegment = NO_ERROR_SEGMENT;
    private long errorRow = NO_ERROR_ROW;

    @Override
    public boolean openForRead() {
        long randomErrorRow =
        numRows = context.getOption(ROWS_PROPERTY, DEFAULT_ROWS, true);
        errorSegment = context.getOption(ERROR_SEGMENT_PROPERTY, NO_ERROR_SEGMENT, true);
        errorRow =  context.getOption(ERROR_ROW_PROPERTY, ThreadLocalRandom.current().nextLong(numRows - 1), true);
        segmentId = context.getSegmentId();
        userDelimiter = String.valueOf(context.getGreenplumCSV().getDelimiter());
        return true;
    }

    @Override
    public OneRow readNextObject() {

        // Termination rule, rows counter in 0-based
        if (row >= numRows) {
            return null;
        }

        // Error rule
        if (segmentId == errorSegment && errorRow == row) {
            throw new PxfRuntimeException(String.format("Test Error Segment-%d Row-%d", segmentId, row));
        }

        // Generate a tuple
        String data = new StringJoiner(userDelimiter)
                .add(String.format("seg-%02d-row-%08d", segmentId, row)) // text
                .add(String.valueOf(row % 1000000000))                // integer (mod 1 billion)
                .add(String.valueOf(row % 2 == 0))                   // boolean
                .add(String.format("%1$d.%1$d1", row))                  // numeric, decimal, real, double precision
                .toString();
        String key = Long.toString(row);

        row++;
        return new OneRow(key, data);
    }

    @Override
    public void closeForRead() {
    }

    @Override
    public boolean openForWrite() {
        throw new UnsupportedOperationException(UNSUPPORTED_ERR_MESSAGE);
    }

    @Override
    public boolean writeNextObject(OneRow onerow) {
        throw new UnsupportedOperationException(UNSUPPORTED_ERR_MESSAGE);
    }

    @Override
    public void closeForWrite() {
        throw new UnsupportedOperationException(UNSUPPORTED_ERR_MESSAGE);
    }
}

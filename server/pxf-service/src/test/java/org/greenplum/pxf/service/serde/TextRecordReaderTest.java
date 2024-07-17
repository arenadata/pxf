package org.greenplum.pxf.service.serde;

import org.greenplum.pxf.api.OneField;
import org.greenplum.pxf.api.model.GreenplumCSV;
import org.greenplum.pxf.api.model.RequestContext;
import org.greenplum.pxf.plugins.hdfs.utilities.PgUtilities;
import org.greenplum.pxf.service.GPDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.DataInput;
import java.io.DataInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TextRecordReaderTest {

    private List<List<OneField>> rows;

    @Mock
    private RequestContext mockRequestContext;
    @Mock
    private GreenplumCSV mockGreenplumCSV;

    @BeforeEach
    private void before() {
        rows = new LinkedList<>();
    }

    @Test
    public void testReadCSVFile() throws Exception {
        runScenario(GPDataGenerator.FORMAT.CSV);
    }

    @Test
    public void testReadTextFile() throws Exception {
        runScenario(GPDataGenerator.FORMAT.TEXT);
    }

    @Test
    public void testReadPipeCSVFile() throws Exception {
        runScenario(GPDataGenerator.FORMAT.CSV_PIPE);
    }

    /**
     * Run the test scenario where the sample data of a given format is read from a previously generated file,
     * deserialized by the TextRecordReader and then compared with the original data values in Java object format.
     * @param format format of data
     * @throws Exception if a problem occurs when reading data
     */
    private void runScenario(GPDataGenerator.FORMAT format) throws Exception {
        when(mockRequestContext.getDatabaseEncoding()).thenReturn(StandardCharsets.UTF_8);
        when(mockRequestContext.getGreenplumCSV()).thenReturn(mockGreenplumCSV);
        when(mockGreenplumCSV.getNewline()).thenReturn("\n");
        when(mockGreenplumCSV.getDelimiter()).thenReturn(format.getDelimiter());
        when(mockGreenplumCSV.getQuote()).thenReturn(format.getQuote());
        when(mockGreenplumCSV.getEscape()).thenReturn(format.getEscape());
        when(mockGreenplumCSV.getValueOfNull()).thenReturn(format.getNil());
        when(mockRequestContext.getTupleDescription()).thenReturn(GPDataGenerator.COLUMN_DESCRIPTORS);
        TextRecordReader reader = new TextRecordReader(mockRequestContext, new PgUtilities());

        // read data from the input stream backed by the file with sample data previously generated by the GPDataGenerator
        DataInput input = new DataInputStream(getClass().getClassLoader().getResourceAsStream("data/" + format.getFilename()));
        List<OneField> record = reader.readRecord(input);
        while (record != null) {
            rows.add(record);
            record = reader.readRecord(input);
        }

        // assert that data read and deserialized matches the data originally generated
        GPDataGenerator.assertDataSet(rows);

    }
}


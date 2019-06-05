package org.greenplum.pxf.plugins.tkh;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.model.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.text.ParseException;

/**
 * JDBC tables accessor
 *
 * The SELECT queries are processed by {@link java.sql.Statement}
 *
 * The INSERT queries are processed by {@link java.sql.PreparedStatement} and
 * built-in JDBC batches of arbitrary size
 */
public class TkhAccessor extends TkhPlugin implements Accessor {

    private static final Logger LOG = LoggerFactory.getLogger(TkhAccessor.class);

    private CloseableHttpClient httpclient;
    private HttpPost request;

    private String query;

    private ByteArrayOutputStream buffer;
    private int bufferTupleSize;

    /**
     * openForWrite() implementation
     * Create query template and open JDBC connection
     *
     * @return true if successful
     */
    @Override
    public boolean openForWrite() throws UnsupportedEncodingException, IOException {
        LOG.debug("openForWrite() called");

        query = buildQuery();
        String requestUrl = buildUrl(host, query);

        httpclient = HttpClients.createDefault();
        request = new HttpPost(requestUrl);

        LOG.debug("openForWrite() httpclient & request created");

        buffer = new ByteArrayOutputStream();
        bufferTupleSize = 0;

        LOG.debug("openForWrite() successful");
        return true;
    }

	/**
     * writeNextObject() implementation
     *
     * If batchSize is not 0 or 1, add a tuple to the batch of statementWrite
     * Otherwise, execute an INSERT query immediately
     *
     * In both cases, a {@link java.sql.PreparedStatement} is used
     *
     * @param row one row
     * @return true if successful
     * @throws SQLException if a database access error occurs
     * @throws IOException if the data provided by {@link JdbcResolver} is corrupted
     * @throws ClassNotFoundException if pooling is used and the JDBC driver was not found
     * @throws IllegalStateException if writerCallableFactory was not properly initialized
     * @throws Exception if it happens in writerCallable.call()
     */
    @Override
    public boolean writeNextObject(OneRow row) throws Exception {
        assert buffer != null;

        LOG.debug("writing to buffer ...");
        buffer.write((byte[])row.getData());
        bufferTupleSize += 1;

        if (bufferTupleSize >= batchSize) {
            send();
            bufferTupleSize = 0;
        }

        return true;
    }

    /**
     * closeForWrite() implementation
     *
     * @throws Exception if it happens in writerCallable.call() or due to runtime errors in thread pool
     */
    @Override
    public void closeForWrite() throws Exception {
        LOG.debug("closeForWrite");
        if (buffer == null || httpclient == null || request == null) {
            return;
        }

        send();
    }

    /**
     * Send buffer to ClickHouse and restart the stream
     */
    private void send() throws IOException, ClientProtocolException {
        assert buffer != null && httpclient != null && request != null;

        if (buffer.size() == 0) {
            return;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(buffer.toByteArray());
        request.setEntity(new InputStreamEntity(bais));

        LOG.debug("Executing request to Clickhouse");

        HttpResponse response = httpclient.execute(request);

        LOG.debug("Clickhouse HttpResponse: {}", response.toString());

        buffer.reset();
    }

    /**
     * Build a SQL INSERT query for Clickhouse
     *
     * @return query
     */
    private String buildQuery() {
        StringBuilder sb = new StringBuilder();

        sb.append(
            "INSERT INTO "
        ).append(
            tableName
        ).append(
            " FORMAT TabSeparated"
        );

        return sb.toString();
    }

    /**
     * Build a URL for query execution in Clickhouse
     *
     * @param hoststring complete host address string (URL and port)
     * @param query SQL query
     * @return URL
     *
     * @throws UnsupportedEncodingException in case URLEncoder fails
     */
    private String buildUrl(String hoststring, String query) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();

        sb.append(
            "http://"
        ).append(
            hoststring
        ).append(
            "/?query="
        ).append(
            URLEncoder.encode(query, "UTF-8")
        );

        return sb.toString();
    }

    @Override
    public boolean openForRead() throws SQLException, SQLTimeoutException, ParseException {
        throw new UnsupportedOperationException("SELECT is not supported");
    }

    @Override
    public OneRow readNextObject() throws SQLException {
        throw new UnsupportedOperationException("SELECT is not supported");
    }

    @Override
    public void closeForRead() throws SQLException {
        // pass, read is not implemented
    }
}

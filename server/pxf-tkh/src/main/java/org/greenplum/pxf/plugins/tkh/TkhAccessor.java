package org.greenplum.pxf.plugins.tkh;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.greenplum.pxf.api.OneRow;
import org.greenplum.pxf.api.model.Accessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Tkh accessor
 */
public class TkhAccessor extends TkhPlugin implements Accessor {
    private static final Logger LOG = LoggerFactory.getLogger(TkhAccessor.class);

    private CloseableHttpAsyncClient asyncHttpClient;
    private List<Future<HttpResponse>> httpResponseFutures = new LinkedList<>();

    private RequestConfig requestConfig;
    private String requestURL;

    private String query;

    private ByteArrayOutputStream buffer;
    private int bufferTupleSize;

    private static final int RESPONSE_CONTENT_MAX_LENGTH = 131072;
    private byte[] responseContent = new byte[RESPONSE_CONTENT_MAX_LENGTH];

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
        requestURL = buildRequestURL(host, query);

        asyncHttpClient = HttpAsyncClients.createDefault();
        asyncHttpClient.start();

        requestConfig = RequestConfig.custom()
            .setSocketTimeout(networkTimeout)
            .setConnectTimeout(networkTimeout)
            .setConnectionRequestTimeout(networkTimeout)
            .build();

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

        buffer.write((byte[])row.getData());
        bufferTupleSize += 1;

        if (bufferTupleSize >= batchSize) {
            LOG.debug("Sending {} tuples from writeNextObject()", bufferTupleSize);
            send();
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
        Exception exceptionToThrow = null;

        if (buffer != null) {
            try {
                if (buffer.size() > 0) {
                    LOG.debug("Sending {} tuples from closeForWrite()", bufferTupleSize);
                    send();
                    LOG.debug("Done, without exceptions");
                }
            }
            catch (Exception e) {
                exceptionToThrow = exceptionToThrow == null ? e : exceptionToThrow;
            }
        }

        if (httpResponseFutures != null) {
            LOG.debug("Collecting and analyzing responses from ClickHouse...");
            for (Future<HttpResponse> httpResponseFuture : httpResponseFutures) {
                try {
                    HttpResponse httpResponse = httpResponseFuture.get();

                    StatusLine responseStatusLine = httpResponse.getStatusLine();
                    if (responseStatusLine.getStatusCode() != 200 || LOG.isDebugEnabled()) {
                        httpResponse.getEntity().getContent().read(responseContent);
                        LOG.warn("Clickhouse response content:\n{}\n", new String(responseContent));

                        String unusualResponseMessage = String.format(
                            "Clickhouse responded with code %s: %s",
                            responseStatusLine.getStatusCode(), responseStatusLine.getReasonPhrase()
                        );
                        if (responseStatusLine.getStatusCode() >= 400 && responseStatusLine.getStatusCode() < 600) {
                            throw new RuntimeException(unusualResponseMessage);
                        }
                        else {
                            LOG.warn(unusualResponseMessage);
                        }
                    }
                }
                catch (Exception e) {
                    exceptionToThrow = exceptionToThrow == null ? e : exceptionToThrow;
                }
            }
            LOG.debug("Done");
        }

        if (asyncHttpClient != null) {
            LOG.debug("Closing HTTP client...");
            try {
                asyncHttpClient.close();
            }
            catch (Exception e) {
                exceptionToThrow = exceptionToThrow == null ? e : exceptionToThrow;
            }
            LOG.debug("Done");
        }

        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
    }

    /**
     * Send buffer to ClickHouse and restart the stream
     */
    private void send() throws IOException, ClientProtocolException {
        assert buffer != null && asyncHttpClient != null;

        if (buffer.size() == 0) {
            return;
        }

        HttpPost request = new HttpPost(requestURL);
        request.setConfig(requestConfig);
        request.setEntity(new ByteArrayEntity(buffer.toByteArray(), ContentType.APPLICATION_OCTET_STREAM));

        httpResponseFutures.add(asyncHttpClient.execute(request, null));

        buffer.reset();
        bufferTupleSize = 0;
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
    private String buildRequestURL(String hoststring, String query) throws UnsupportedEncodingException {
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

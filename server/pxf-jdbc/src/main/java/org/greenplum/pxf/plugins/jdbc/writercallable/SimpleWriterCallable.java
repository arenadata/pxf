package org.greenplum.pxf.plugins.jdbc.writercallable;

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
import org.greenplum.pxf.plugins.jdbc.JdbcResolver;
import org.greenplum.pxf.plugins.jdbc.JdbcBasePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This writer makes simple, one-by-one INSERTs.
 * A call() is required after every supply()
 */
class SimpleWriterCallable implements WriterCallable {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleWriterCallable.class);
    private final JdbcBasePlugin plugin;
    private final String query;
    private OneRow row;
    private final Runnable onComplete;

    SimpleWriterCallable(JdbcBasePlugin plugin, String query, Runnable onComplete) {
        if ((plugin == null) || (query == null)) {
            throw new IllegalArgumentException("The provided JdbcBasePlugin or SQL query is null");
        }
        this.plugin = plugin;
        this.query = query;
        this.onComplete = onComplete;
        row = null;
    }

    @Override
    public void supply(OneRow row) throws IllegalStateException {
        if (this.row != null) {
            throw new IllegalStateException("Trying to supply() a OneRow object to a full WriterCallable");
        }
        if (row == null) {
            throw new IllegalArgumentException("Trying to supply() a null OneRow object");
        }
        this.row = row;
    }

    @Override
    public boolean isCallRequired() {
        return this.row != null;
    }

    @Override
    public SQLException call() throws IOException, SQLException {
        LOG.trace("Writer {}: call() to insert row", this);
        long start = System.nanoTime();
        if (row == null) {
            return null;
        }

        PreparedStatement statement = null;
        SQLException res = null;
        try {
            statement = plugin.getPreparedStatement(plugin.getConnection(), query);
            LOG.trace("Writer {}: got statement", this);
            JdbcResolver.decodeOneRowToPreparedStatement(row, statement);
            statement.executeUpdate();
            // some drivers will not react to timeout interrupt
            if (Thread.interrupted())
                throw new SQLException("Writer was interrupted by timeout");
        } catch (Throwable t) {
            if (t instanceof SQLException)
                res = (SQLException) t;
            else if (t.getCause() instanceof SQLException)
                res = (SQLException) t.getCause();
            else
                res = new SQLException(t);
            return res;
        } finally {
            if (LOG.isTraceEnabled()) {
                long duration = System.nanoTime() - start;
                LOG.trace("Writer {}: call() done in {} ms, exception={}", this, duration / 1000000, res);
            }
            row = null;
            JdbcBasePlugin.closeStatementAndConnection(statement);
            LOG.trace("Writer {} completed inserting raw. Release the semaphore", this);
            onComplete.run();
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("SimpleWriterCallable@%d", hashCode());
    }
}

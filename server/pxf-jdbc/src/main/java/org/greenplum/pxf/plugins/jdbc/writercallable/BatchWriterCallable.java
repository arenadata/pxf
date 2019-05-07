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
import org.greenplum.pxf.plugins.jdbc.JdbcBasePlugin;
import org.greenplum.pxf.plugins.jdbc.JdbcResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.BatchUpdateException;
import java.util.LinkedList;
import java.util.List;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * This writer makes batch INSERTs.
 *
 * A call() is required after a certain number of supply() calls
 */
class BatchWriterCallable implements WriterCallable {
    @Override
    public boolean supply(OneRow row) {
        rows.add(row);
        return rows.size() >= batchSize;
    }

    @Override
    public SQLException call() throws IOException, SQLException, ClassNotFoundException {
        if (rows.isEmpty()) {
            return null;
        }

        long startTime = 0;
        if (LOG.isDebugEnabled()) {
            startTime = System.nanoTime();
        }

        boolean statementMustBeDeleted = false;
        if (statement == null) {
            statement = plugin.getPreparedStatement(plugin.getConnection(), query);
            if (LOG.isDebugEnabled()) {
                double elapsedTime = (System.nanoTime() - startTime) / 1000000.0;
                LOG.debug("Time to create connection and statement for batch: {}ms", elapsedTime);
                startTime = System.nanoTime();
            }
            statementMustBeDeleted = true;
        }

        long dortpsStart;
        long dortpsCurrent;
        long dortpsTotal = 0;
        long dortpsMin = Long.MAX_VALUE;
        long dortpsMax = 0;
        for (OneRow row : rows) {
            dortpsStart = System.nanoTime();
            JdbcResolver.decodeOneRowToPreparedStatement(row, statement);
            dortpsCurrent = System.nanoTime() - dortpsStart;
            dortpsTotal += dortpsCurrent;
            dortpsMin = dortpsMin < dortpsCurrent ? dortpsMin : dortpsCurrent;
            dortpsMax = dortpsMax > dortpsCurrent ? dortpsMax : dortpsCurrent;
            statement.addBatch();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("DORTPS Total: {}ms", dortpsTotal / 1000000.0);
            LOG.debug("DORTPS Min: {}ms", dortpsMin / 1000000.0);
            LOG.debug("DORTPS Max: {}ms", dortpsMax / 1000000.0);
            LOG.debug("DORTPS Avg: {}ms", dortpsTotal / (1000000.0 * rows.size()));
            LOG.debug("DORTPS row count: {}", rows.size());
        }

        try {
            statement.executeBatch();
        }
        catch (BatchUpdateException bue) {
            SQLException cause = bue.getNextException();
            return cause != null ? cause : bue;
        }
        catch (SQLException e) {
            return e;
        }
        finally {
            rows.clear();
            if (statementMustBeDeleted) {
                JdbcBasePlugin.closeStatementAndConnection(statement);
                statement = null;
            }
            if (LOG.isDebugEnabled()) {
                double elapsedTime = (System.nanoTime() - startTime) / 1000000.0;
                LOG.debug("Time to fill and execute batch: {}ms", elapsedTime);
            }
        }

        return null;
    }

    /**
     * Construct a new batch writer
     */
    BatchWriterCallable(JdbcBasePlugin plugin, String query, PreparedStatement statement, int batchSize) {
        if (plugin == null || query == null) {
            throw new IllegalArgumentException("The provided JdbcBasePlugin or SQL query is null");
        }

        this.plugin = plugin;
        this.query = query;
        this.statement = statement;
        this.batchSize = batchSize;

        rows = new LinkedList<>();
    }

    private static final Logger LOG = LoggerFactory.getLogger(BatchWriterCallable.class);

    private final JdbcBasePlugin plugin;
    private final String query;
    private PreparedStatement statement;
    private List<OneRow> rows;
    private final int batchSize;
}

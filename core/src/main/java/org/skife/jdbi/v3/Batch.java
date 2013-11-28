/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v3;

import org.skife.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.skife.jdbi.v3.tweak.SQLLog;
import org.skife.jdbi.v3.tweak.StatementRewriter;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of non-prepared statements to be sent to the RDMBS in one "request"
 */
public class Batch extends BaseStatement
{
    private List<String> parts = new ArrayList<String>();
    private final StatementRewriter rewriter;
    private final Connection connection;
    private final SQLLog log;
    private final TimingCollector timingCollector;

    Batch(StatementRewriter rewriter,
          Connection connection,
          Map<String, Object> globalStatementAttributes,
          SQLLog log,
          TimingCollector timingCollector,
          Foreman foreman)
    {
        super(new ConcreteStatementContext(globalStatementAttributes), foreman);
        this.rewriter = rewriter;
        this.connection = connection;
        this.log = log;
        this.timingCollector = timingCollector;
    }

    /**
     * Add a statement to the batch
     *
     * @param sql SQL to be added to the batch, possibly a named statement
     * @return the same Batch statement
     */
    public Batch add(String sql)
    {
        parts.add(sql);
        return this;
    }

    /**
     * Specify a value on the statement context for this batch
     *
     * @return self
     */
    public Batch define(String key, Object value) {
        getContext().setAttribute(key, value);
        return this;
    }

    /**
     * Execute all the queued up statements
     *
     * @return an array of integers representing the return values from each statement's execution
     */
    public int[] execute()
    {
        // short circuit empty batch
        if (parts.size() == 0) return new int[] {};

        Binding empty = new Binding();
        Statement stmt = null;
        try
        {
            try
            {
                stmt = connection.createStatement();
                addCleanable(Cleanables.forStatement(stmt));
            }
            catch (SQLException e)
            {
                throw new UnableToCreateStatementException(e, getContext());
            }

            final SQLLog.BatchLogger logger = log.logBatch();
            try
            {
                for (String part : parts)
                {
                    final String sql = rewriter.rewrite(part, empty, getContext()).getSql();
                    logger.add(sql);
                    stmt.addBatch(sql);
                }
            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException("Unable to configure JDBC statement", e, getContext());
            }

            try
            {
                final long start = System.nanoTime();
                final int[] rs = stmt.executeBatch();
                final long elapsedTime = System.nanoTime() - start;
                logger.log(elapsedTime / 1000000L);
                // Null for statement, because for batches, we don't really have a good way to keep the sql around.
                timingCollector.collect(elapsedTime, getContext());
                return rs;

            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException(e, getContext());
            }
        }
        finally {
            cleanup();
        }
    }
}

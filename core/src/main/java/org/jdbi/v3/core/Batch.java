/*
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
package org.jdbi.v3.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a group of non-prepared statements to be sent to the RDMBS in one "request"
 */
public class Batch extends BaseStatement
{
    private static final Logger LOG = LoggerFactory.getLogger(Batch.class);

    private final List<String> parts = new ArrayList<>();
    private final Connection connection;

    Batch(JdbiConfig config,
          Connection connection,
          ConcreteStatementContext statementContext)
    {
        super(config, statementContext);
        this.connection = connection;
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
     * Define a value on the {@link StatementContext}.
     *
     * @param key   Key to access this value from the StatementContext
     * @param value Value to setAttribute on the StatementContext
     *
     * @return this
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
        if (parts.size() == 0) {
            return new int[] {};
        }

        Binding empty = new Binding();
        Statement stmt;
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

            LOG.trace("Execute batch [");

            try
            {
                for (String part : parts)
                {
                    final String sql = config.statementRewriter.rewrite(part, empty, getContext()).getSql();
                    LOG.trace("  {}", sql);
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
                LOG.trace("] executed in {}ms", elapsedTime / 1000000L);
                // Null for statement, because for batches, we don't really have a good way to keep the sql around.
                config.timingCollector.collect(elapsedTime, getContext());
                return rs;

            }
            catch (SQLException e)
            {
                throw new UnableToExecuteStatementException(mungeBatchException(e), getContext());
            }
        }
        finally {
            cleanup();
        }
    }

    /**
     * SQLExceptions thrown from batch executions have errors
     * in a {@link SQLException#getNextException()} chain, which
     * doesn't print out when you log them.  Convert them to be
     * {@link Throwable#addSuppressed(Throwable)} exceptions,
     * which do print out with common logging frameworks.
     */
    static SQLException mungeBatchException(SQLException e) {
        for (SQLException next = e.getNextException(); next != null; next = next.getNextException()) {
            e.addSuppressed(next);
        }
        return e;
    }
}

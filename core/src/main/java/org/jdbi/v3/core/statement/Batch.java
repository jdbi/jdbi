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
package org.jdbi.v3.core.statement;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a group of non-prepared statements to be sent to the RDMBS in one "request".
 */
public class Batch extends AbstractBaseStatement<Batch> {
    private static final Logger LOG = LoggerFactory.getLogger(Batch.class);

    private final List<String> parts = new ArrayList<>();

    public Batch(Handle handle) {
        super(handle);
    }

    /**
     * Add a statement to the batch
     *
     * @param sql SQL to be added to the batch, possibly a named statement
     * @return the same Batch statement
     */
    public Batch add(String sql) {
        parts.add(sql);
        return this;
    }

    /**
     * Execute all the queued up statements
     *
     * @return an array of integers representing the return values from each statement's execution
     */
    public int[] execute() {
        // short circuit empty batch
        if (parts.isEmpty()) {
            return new int[] {};
        }

        @SuppressWarnings("PMD.CloseResource")
        Statement stmt;
        try {
            try {
                stmt = getHandle().getStatementBuilder().create(getHandle().getConnection(), getContext());
                addCleanable(stmt::close);
            } catch (SQLException e) {
                throw new UnableToCreateStatementException(e, getContext());
            }

            LOG.trace("Execute batch [");

            try {
                for (String part : parts) {
                    final String sql = getConfig(SqlStatements.class).getTemplateEngine().render(part, getContext());
                    LOG.trace(" {}", sql);
                    stmt.addBatch(sql);
                }
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Unable to configure JDBC statement", e, getContext());
            }

            try {
                return SqlLoggerUtil.wrap(stmt::executeBatch, getContext(), getConfig(SqlStatements.class).getSqlLogger());
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException(mungeBatchException(e), getContext());
            }
        } finally {
            close();
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

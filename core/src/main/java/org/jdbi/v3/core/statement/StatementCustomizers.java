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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Some simple {@link StatementCustomizer}s you might find handy.
 */
public final class StatementCustomizers {
    private StatementCustomizers() { }

    /**
     * Set a timeout on the statement.
     * @param seconds timeout in seconds
     * @return the customizer
     */
    public static StatementCustomizer statementTimeout(final int seconds) {
        return new StatementCustomizer() {
            @Override
            public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException {
                stmt.setQueryTimeout(seconds);
            }
        };
    }

    /**
     * Set the number of rows to fetch from the database in a single batch.
     * In a transaction, may enable streaming result sets instead of buffering in memory.
     * @param fetchSize number of rows to fetch at a time
     * @return the customizer
     */
    public static StatementCustomizer fetchSize(final int fetchSize) {
        return new StatementCustomizer() {
            @Override
            public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException {
                stmt.setFetchSize(fetchSize);
            }
        };
    }

    /**
     * Limit number of rows returned.  Note that this may be significantly
     * less efficient than doing it in the SQL with a {@code LIMIT} clause
     * and is not recommended unless you understand why you need it specifically.
     * @param maxRows number of rows to return
     * @return the customizer
     */
    public static StatementCustomizer maxRows(final int maxRows) {
        return new StatementCustomizer() {
            @Override
            public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException {
                stmt.setMaxRows(maxRows);
            }
        };
    }

    /**
     * Sets the limit of large variable size types like {@code VARCHAR}.
     * Limited data is silently discarded, so be careful!
     * @param maxFieldSize the maximum field size to return
     * @return the customizer
     */
    public static StatementCustomizer maxFieldSize(final int maxFieldSize) {
        return new StatementCustomizer() {
            @Override
            public void beforeExecution(final PreparedStatement stmt, final StatementContext ctx) throws SQLException {
                stmt.setMaxFieldSize(maxFieldSize);
            }
        };
    }
}

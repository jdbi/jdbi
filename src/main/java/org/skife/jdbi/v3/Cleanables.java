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

import org.skife.jdbi.v3.tweak.StatementBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Resource management for JDBI. Cleanables can be registered on a SQL statement and they get cleaned up when the
 * statement finishes or (in the case of a ResultIterator), the object representing the results is closed.
 *
 * Resources managed by JDBI are {@link ResultSet}, {@link Statement}, {@link Handle} and {@link StatementBuilder} for historical reasons.
 */
class Cleanables
{
    static Cleanable forResultSet(final ResultSet rs) {
        return new Cleanable() {
            public void cleanup() throws SQLException {
                if (rs != null) {
                    rs.close();
                }
            }
        };
    }

    static Cleanable forStatement(final Statement stmt) {
        return new Cleanable() {
            public void cleanup() throws SQLException {
                if (stmt != null) {
                    stmt.close();
                }
            }
        };
    }

    static Cleanable forHandle(final Handle handle, final TransactionState state) {
        return new Cleanable() {
            public void cleanup() throws SQLException {
                if (handle != null) {
                    if (handle.isInTransaction()) {
                        if (state == TransactionState.COMMIT) {
                            handle.commit();
                        }
                        else {
                            handle.rollback();
                        }
                    }

                    handle.close();
                }
            }
        };
    }

    /**
     * In the {@link SQLStatement} derived classes, the {@link Statement} is not managed directly but through the
     * {@link StatementBuilder}, which allows the {@link CachingStatementBuilder} to hook in and provide {@link PreparedStatement} caching.
     */
    static class StatementBuilderCleanable implements Cleanable
    {
        private final StatementBuilder statementBuilder;
        private final Connection conn;
        private final String sql;
        private final Statement stmt;

        StatementBuilderCleanable(final StatementBuilder statementBuilder,
                                  final Connection conn,
                                  final String sql,
                                  final Statement stmt)
        {
            this.statementBuilder = statementBuilder;
            this.conn = conn;
            this.sql = sql;
            this.stmt = stmt;
        }


        public void cleanup() throws SQLException
        {
            if (statementBuilder != null) {
                statementBuilder.close(conn, sql, stmt);
            }
        }
    }
}

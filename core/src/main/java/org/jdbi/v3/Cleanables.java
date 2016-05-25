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
package org.jdbi.v3;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jdbi.v3.tweak.StatementBuilder;

/**
 * Resource management for JDBI. Cleanables can be registered on a SQL statement and they get cleaned up when the
 * statement finishes or (in the case of a ResultIterator), the object representing the results is closed.
 *
 * Resources managed by JDBI are {@link ResultSet}, {@link Statement}, {@link Handle} and {@link StatementBuilder} for historical reasons.
 */
class Cleanables
{
    private Cleanables()
    {
        throw new AssertionError("do not instantiate");
    }

    static Cleanable forResultSet(final ResultSet rs)
    {
        return new ResultSetCleanable(rs);
    }

    static Cleanable forStatement(final Statement stmt)
    {
        return new StatementCleanable(stmt);
    }

    static Cleanable forHandle(final Handle handle, final TransactionState state)
    {
        return new HandleCleanable(handle, state);
    }

    private static final class ResultSetCleanable implements Cleanable
    {
        private final ResultSet rs;

        private ResultSetCleanable(ResultSet rs)
        {
            this.rs = rs;
        }

        @Override
        public void cleanup()
            throws SQLException
        {
            if (rs != null) {
                rs.close();
            }
        }

        @Override
        public int hashCode()
        {
            return rs == null ? 0 : rs.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) {
                return true;
            }
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }

            ResultSetCleanable that = (ResultSetCleanable) o;

            if (this.rs == null) {
                return that.rs == null;
            }
            return this.rs.equals(that.rs);
        }
    }

    private static final class StatementCleanable implements Cleanable
    {
        private final Statement stmt;

        private StatementCleanable(Statement stmt)
        {
            this.stmt = stmt;
        }

        @Override
        public void cleanup()
            throws SQLException
        {
            if (stmt != null) {
                stmt.close();
            }
        }

        @Override
        public int hashCode()
        {
            return stmt == null ? 0 : stmt.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) {
                return true;
            }
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }

            StatementCleanable that = (StatementCleanable) o;

            if (this.stmt == null) {
                return that.stmt == null;
            }
            return this.stmt.equals(that.stmt);
        }
    }

    private static final class HandleCleanable implements Cleanable
    {
        private final Handle handle;
        private final TransactionState state;

        private HandleCleanable(Handle handle, TransactionState state)
        {
            this.handle = handle;
            this.state = state;
        }

        @Override
        public void cleanup()
            throws SQLException
        {
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

        @Override
        public int hashCode()
        {
            return handle == null ? 0 : handle.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) {
                return true;
            }
            if (o == null || o.getClass() != this.getClass()) {
                return false;
            }

            HandleCleanable that = (HandleCleanable) o;

            if (this.handle == null) {
                return that.handle == null;
            }
            return this.handle.equals(that.handle);
        }
    }

    /**
     * In the {@link SqlStatement} derived classes, the {@link Statement} is not managed directly but through the
     * {@link StatementBuilder}.
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

        @Override
        public void cleanup()
            throws SQLException
        {
            if (statementBuilder != null) {
                statementBuilder.close(conn, sql, stmt);
            }
        }
    }
}

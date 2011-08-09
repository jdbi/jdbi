package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementBuilder;

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
    interface Cleanable
    {
        void cleanup() throws SQLException;
    }

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

    static Cleanable forHandle(final Handle handle) {
        return new Cleanable() {
            public void cleanup() throws SQLException {
                if (handle != null) {
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

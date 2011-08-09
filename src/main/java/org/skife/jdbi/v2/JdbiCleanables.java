package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementBuilder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

class JdbiCleanables
{
    interface JdbiCleanable
    {
        void cleanup() throws SQLException;
    }

    static JdbiCleanable forResultSet(final ResultSet rs) {
        return new JdbiCleanable() {
            public void cleanup() throws SQLException {
                if (rs != null) {
                    rs.close();
                }
            }
        };
    }

    static JdbiCleanable forStatement(final Statement stmt) {
        return new JdbiCleanable() {
            public void cleanup() throws SQLException {
                if (stmt != null) {
                    stmt.close();
                }
            }
        };
    }

    static JdbiCleanable forConnection(final Connection conn) {
        return new JdbiCleanable() {
            public void cleanup() throws SQLException {
                if (conn != null) {
                    conn.close();
                }
            }
        };
    }

    static class StatementBuilderCleanable implements JdbiCleanable
    {
        private final StatementBuilder statementBuilder;
        private final Connection conn;
        private final String sql;
        private final Statement stmt;

        StatementBuilderCleanable(StatementBuilder statementBuilder, Connection conn, String sql, Statement stmt)
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

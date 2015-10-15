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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jdbi.v3.tweak.StatementBuilder;

/**
 * A StatementBuilder which will always create a new PreparedStatement
 */
public class DefaultStatementBuilder implements StatementBuilder
{
    /**
     * Create a new DefaultStatementBuilder which will always create a new PreparedStatement from
     * the Connection
     *
     * @param conn Used to prepare the statement
     * @param sql  Translated SQL statement
     * @param ctx  Unused
     *
     * @return a new PreparedStatement
     */
    @Override
    public PreparedStatement create(Connection conn, String sql, StatementContext ctx) throws SQLException
    {
        if (ctx.isReturningGeneratedKeys()) {
            String[] columnNames = ctx.getGeneratedKeysColumnNames();
            if (columnNames != null && columnNames.length > 0) {
                return conn.prepareStatement(sql, columnNames);
            }
            return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        }
        else if (ctx.isConcurrentUpdatable()) {
            return conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
        }
        else {
            return conn.prepareStatement(sql);
        }
    }

    /**
     * Called to close an individual prepared statement created from this builder.
     * In this case, it closes imemdiately
     *
     * @param sql  the translated SQL which was prepared
     * @param stmt the statement
     *
     * @throws java.sql.SQLException if anything goes wrong closing the statement
     */
    @Override
    public void close(Connection conn, String sql, Statement stmt) throws SQLException
    {
        if (stmt != null) {
            stmt.close();
        }
    }

    /**
     * In this case, a NOOP
     */
    @Override
    public void close(Connection conn)
    {
    }

    /**
     * Called each time a Callable statement needs to be created
     *
     * @param conn the JDBC Connection the statement is being created for
     * @param sql the translated SQL which should be prepared
     * @param ctx Statement context associated with the SQLStatement this is building for
     */
    @Override
    public CallableStatement createCall(Connection conn, String sql, StatementContext ctx) throws SQLException
    {
        return conn.prepareCall(sql);
    }
}

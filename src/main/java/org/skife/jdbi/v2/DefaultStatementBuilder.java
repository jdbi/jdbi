/*
 * Copyright (C) 2004 - 2014 Brian McCallister
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
package org.skife.jdbi.v2;

import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.tweak.StatementBuilder;

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
    public PreparedStatement create(Connection conn, String sql, StatementContext ctx) throws SQLException
    {
        if (ctx.isReturningGeneratedKeys()) {
            final Method daoMethod = ctx.getSqlObjectMethod();
            if (daoMethod != null) {
                final GetGeneratedKeys ggk = daoMethod.getAnnotation(GetGeneratedKeys.class);
                if (ggk != null) {
                    final String[] columns = ggk.columns();
                    if (columns != null && columns.length > 0) {
                        return conn.prepareStatement(sql, columns);
                    }
                }
            }

            return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
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
    public void close(Connection conn, String sql, Statement stmt) throws SQLException
    {
        if (stmt != null) {
            stmt.close();
        }
    }

    /**
     * In this case, a NOOP
     */
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
    public CallableStatement createCall(Connection conn, String sql, StatementContext ctx) throws SQLException
    {
        return conn.prepareCall(sql);
    }
}

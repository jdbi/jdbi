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
package org.skife.jdbi.v2;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.skife.jdbi.v2.tweak.StatementBuilder;

/**
 * A StatementBuilder which decorates another StatementBuilder and caches
 * @deprecated This should be done in the JDBC driver, not here
 */
@Deprecated
public class CachingStatementBuilder implements StatementBuilder
{
    private Map<String, PreparedStatement> cache = new HashMap<String, PreparedStatement>();
    private final StatementBuilder builder;

    /**
     * Create a new CachingStatementBuilder which decorates the one passed in.
     *
     * @param builder The StatementBuilder used to actual PreparedStatement creation
     */
    public CachingStatementBuilder(StatementBuilder builder) {
        this.builder = builder;
    }

    /**
     * Return either a cached PreparedStatement or a new one which has just been added to the cache
     * @return A new, or cached, PreparedStatement
     */
    public PreparedStatement create(Connection conn, String sql, StatementContext ctx) throws SQLException
    {
        if (cache.containsKey(sql)) {
            PreparedStatement cached = cache.get(sql);
            cached.clearParameters();
            return cached;
        }

        PreparedStatement stmt = builder.create(conn, sql, ctx);
        cache.put(sql, stmt);
        return stmt;
    }

    /**
     * NOOP, statements will be closed when the handle is closed
     */
    public void close(Connection conn, String sql, Statement stmt) throws SQLException
    {
    }

    /**
     * Iterate over all cached statements and ask the wrapped StatementBuilder to close
     * each one.
     */
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void close(Connection conn)
    {
        for (Map.Entry<String,PreparedStatement> statement : cache.entrySet()) {
            try {
                builder.close(conn, statement.getKey(), statement.getValue());
            }
            catch (SQLException e) {
                // nothing we can do!
            }
        }
    }

    public CallableStatement createCall(Connection conn, String sql, StatementContext ctx) throws SQLException
    {
        if (cache.containsKey(sql)) {
            CallableStatement cached = (CallableStatement) cache.get(sql);
            cached.clearParameters();
            return cached;
        }

        CallableStatement stmt = builder.createCall(conn, sql, ctx);
        cache.put(sql, stmt);
        return stmt;
    }
}

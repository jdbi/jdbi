/*
 * Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
class PreparedStatementCache implements StatementBuilder
{
    private final Connection conn;

    private Map<String, PreparedStatement> cache = new HashMap<String, PreparedStatement>();

    PreparedStatementCache(Connection conn)
    {
        this.conn = conn;
    }

    public PreparedStatement create(String sql, StatementContext ctx) throws SQLException
    {
        if (cache.containsKey(sql)) {
            PreparedStatement cached = cache.get(sql);
            cached.clearParameters();
            return cached;
        }

        PreparedStatement stmt = conn.prepareStatement(sql);
        cache.put(sql, stmt);
        return stmt;
    }

    public void close()
    {
        for (PreparedStatement statement : cache.values())
        {
            try
            {
                statement.close();
            }
            catch (SQLException e)
            {
                // nothing we can do!
            }
        }
    }
}

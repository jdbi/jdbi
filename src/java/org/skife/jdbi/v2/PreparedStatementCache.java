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

    public PreparedStatement create(String sql) throws SQLException
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

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementBuilderFactory;
import org.skife.jdbi.v2.tweak.StatementBuilder;

import java.sql.Connection;

/**
 * The default StatementBuilderFactory. Provides StatementBuilder instances
 * which cache all prepared statements for a given handle instance.
 */
public class CachingStatementBuilderFactory implements StatementBuilderFactory
{
    /**
     * Return a new, or cached, prepared statement
     */
    public StatementBuilder createStatementBuilder(Connection conn) {
        return new PreparedStatementCache(conn);
    }
}

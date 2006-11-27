package org.skife.jdbi.v2.tweak;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Used to convert translated SQL into a prepared statement. The default implementation
 * created by {@link org.skife.jdbi.v2.CachingStatementBuilderFactory} caches all prepared
 * statements created against a given handle.
 * @see StatementBuilderFactory
 */
public interface StatementBuilder
{
    /**
     * Called each time a prepared statement needs to be created
     * @param sql the translated SQL which should be prepared
     */
    PreparedStatement create(String sql) throws SQLException;

    /**
     * Called when the handle this StatementBuilder is attached to is closed.
     */
    void close();
}

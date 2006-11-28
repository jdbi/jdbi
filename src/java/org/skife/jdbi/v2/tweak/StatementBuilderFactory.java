package org.skife.jdbi.v2.tweak;

import java.sql.Connection;

/**
 * Used to specify how prepared statements are built. A factry is attached to a DBI instance, and
 * whenever the DBI instance is used to create a Handle the factory will be used to create a
 * StatementBuilder for that specific handle.
 * <p>
 * The default implementation caches all prepared statements for a given Handle instance. To change
 * the factory, use {@link org.skife.jdbi.v2.DBI#setStatementBuilderFactory(StatementBuilderFactory)}.
 */
public interface StatementBuilderFactory
{
    /**
     * Obtain a StatementBuilder, called when a new handle is opened
     */
    StatementBuilder createStatementBuilder(Connection conn);
}

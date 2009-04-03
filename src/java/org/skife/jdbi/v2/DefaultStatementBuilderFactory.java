package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementBuilder;
import org.skife.jdbi.v2.tweak.StatementBuilderFactory;

import java.sql.Connection;

/**
 *
 */
public class DefaultStatementBuilderFactory implements StatementBuilderFactory
{
    /**
     * Obtain a StatementBuilder, called when a new handle is opened
     */
    public StatementBuilder createStatementBuilder(Connection conn)
    {
        return new DefaultStatementBuilder();
    }
}

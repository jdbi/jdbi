package org.skife.jdbi.v2.tweak;

import java.sql.Connection;

/**
 *
 */
public interface StatementBuilderFactory
{
    StatementBuilder createStatementBuilder(Connection conn);    
}

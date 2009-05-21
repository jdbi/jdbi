package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.StatementContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Convenience class which provides no-op stubs of the StatementCustomizer methods
 */
public class BaseStatementCustomizer implements StatementCustomizer
{
    /**
     * Make the changes you need to inside this method. It will be invoked prior to execution of
     * the prepared statement
     *
     * @param stmt Prepared statement being customized
     * @param ctx  Statement context associated with the statement being customized
     *
     * @throws java.sql.SQLException go ahead and percolate it for jDBI to handle
     */
    public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
    {
    }

    /**
     * This will be invoked after execution of the prepared statement, but before any results
     * are accessed.
     *
     * @param stmt Prepared statement being customized
     * @param ctx  Statement context associated with the statement being customized
     *
     * @throws java.sql.SQLException go ahead and percolate it for jDBI to handle
     */
    public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
    {
    }
}

package org.skife.jdbi.v2.tweak;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Used t customize a prepared statement.
 */
public interface StatementCustomizer
{
    /**
     * Make the changes you need to inside this method.
     *
     * @param stmt Prepared statement being customized
     * @throws SQLException go ahead and percolate it for jDBI to handle
     */
    public void customize(PreparedStatement stmt) throws SQLException;
}

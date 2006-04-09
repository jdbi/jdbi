package org.skife.jdbi.v2.tweak;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 
 */
public interface StatementCustomizer
{
    public void customize(PreparedStatement stmt) throws SQLException;
}

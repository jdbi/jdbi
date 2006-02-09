package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Represents an argument to a prepared statement
 */
interface Argument
{
    void apply(int position, PreparedStatement statement) throws SQLException;
}

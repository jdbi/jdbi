package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 
 */
class StringArgument implements Argument
{
    private final String value;

    StringArgument(String value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setString(position, value);
    }
}

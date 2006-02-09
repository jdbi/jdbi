package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class IntegerArgument implements Argument
{
    private final int value;

    IntegerArgument(int value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setInt(position, value);
    }
}

package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public class BooleanArgument implements Argument
{
    private final boolean value;

    BooleanArgument(boolean value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setBoolean(position, value);
    }
}

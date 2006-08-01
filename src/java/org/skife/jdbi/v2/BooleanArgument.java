package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

class BooleanArgument implements Argument
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

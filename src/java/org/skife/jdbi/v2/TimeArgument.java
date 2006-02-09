package org.skife.jdbi.v2;

import java.sql.Time;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class TimeArgument implements Argument
{
    private final Time value;

    TimeArgument(Time value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setTime(position, value);
    }
}

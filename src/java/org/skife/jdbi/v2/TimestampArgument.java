package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class TimestampArgument implements Argument
{
    private final Timestamp value;

    TimestampArgument(Timestamp value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setTimestamp(position, value);
    }
}

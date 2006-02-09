package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class LongArgument implements Argument
{
    private final long value;

    LongArgument(long value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setLong(position, value);
    }
}

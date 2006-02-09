package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class ByteArgument implements Argument
{
    private final byte value;

    ByteArgument(byte value)
    {

        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setByte(position, value);
    }
}

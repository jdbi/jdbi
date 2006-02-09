package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class ByteArrayArgument implements Argument
{
    private final byte[] value;

    ByteArrayArgument(byte[] value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setBytes(position, value);
    }
}

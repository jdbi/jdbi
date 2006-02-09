package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class InputStreamArgument implements Argument
{
    private final InputStream value;
    private final int length;
    private final boolean ascii;

    InputStreamArgument(InputStream value, int length, boolean ascii)
    {
        this.value = value;
        this.length = length;
        this.ascii = ascii;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        if (ascii)
        {
            statement.setAsciiStream(position, value, length);
        }
        else
        {
            statement.setBinaryStream(position, value, length);
        }
    }
}

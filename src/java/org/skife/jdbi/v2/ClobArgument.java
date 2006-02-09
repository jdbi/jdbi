package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
public class ClobArgument implements Argument
{
    private final Clob value;

    ClobArgument(Clob value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setClob(position, value);
    }
}

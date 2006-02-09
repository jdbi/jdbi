package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class FloatArgument implements Argument
{
    private final Float value;

    FloatArgument(Float value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setFloat(position, value);
    }
}

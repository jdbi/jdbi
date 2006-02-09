package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class DoubleArgument implements Argument
{
    private final Double value;

    DoubleArgument(Double value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setDouble(position, value);
    }
}

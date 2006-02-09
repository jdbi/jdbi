package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class ObjectArgument implements Argument
{
    private final Object value;

    ObjectArgument(Object value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setObject(position, value);
    }
}

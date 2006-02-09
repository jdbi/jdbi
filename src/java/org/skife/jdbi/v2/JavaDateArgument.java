package org.skife.jdbi.v2;

import java.util.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class JavaDateArgument implements Argument
{
    private final Date value;

    JavaDateArgument(Date value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setDate(position, new java.sql.Date(value.getTime()));
    }
}

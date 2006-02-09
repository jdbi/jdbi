package org.skife.jdbi.v2;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class SqlDateArgument implements Argument
{
    private final Date value;

    SqlDateArgument(Date value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setDate(position, value);
    }
}

package org.skife.jdbi.v2;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.Argument;

/**
 * Takes a boolean and converts it into integer 0/1 column values. This is useful if your database does 
 * not support boolean column types.
 */
class BooleanIntegerArgument implements Argument
{
    private final boolean value;

    BooleanIntegerArgument(final boolean value)
    {
        this.value = value;
    }

    public void apply(final int position, final PreparedStatement statement, final StatementContext ctx) throws SQLException 
    {
        statement.setInt(position, value ? 1 : 0);
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }
}

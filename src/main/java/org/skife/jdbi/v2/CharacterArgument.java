package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;

class CharacterArgument implements Argument
{
    private final Character value;

    CharacterArgument(Character value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException
    {
        statement.setString(position, String.valueOf(value));
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
package org.skife.jdbi.v2;

import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class CharacterStreamArgument implements Argument
{
    private final Reader value;
    private final int length;

    CharacterStreamArgument(Reader value, int length)
    {
        this.value = value;
        this.length = length;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setCharacterStream(position, value, length);
    }
}

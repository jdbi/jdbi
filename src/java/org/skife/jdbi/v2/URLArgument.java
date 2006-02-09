package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.Argument;

import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 */
class URLArgument implements Argument
{
    private final URL value;

    URLArgument(URL value)
    {
        this.value = value;
    }

    public void apply(int position, PreparedStatement statement) throws SQLException
    {
        statement.setURL(position, value);
    }
}

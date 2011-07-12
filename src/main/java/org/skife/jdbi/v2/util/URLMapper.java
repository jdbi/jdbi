package org.skife.jdbi.v2.util;

import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;

public class URLMapper extends TypedMapper<URL>
{

    public URLMapper()
    {
        super();
    }

    public URLMapper(int index)
    {
        super(index);
    }

    public URLMapper(String name)
    {
        super(name);
    }

    @Override
    protected URL extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getURL(name);
    }

    @Override
    protected URL extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getURL(index);
    }

    public static final URLMapper FIRST = new URLMapper();
}

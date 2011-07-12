package org.skife.jdbi.v2.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ShortMapper extends TypedMapper<Short>
{
    public ShortMapper()
    {
        super();
    }

    public ShortMapper(int index)
    {
        super(index);
    }

    public ShortMapper(String name)
    {
        super(name);
    }

    @Override
    protected Short extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getShort(name);
    }

    @Override
    protected Short extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getShort(index);
    }

    public static final ShortMapper FIRST = new ShortMapper();
}

package org.skife.jdbi.v2.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FloatMapper extends TypedMapper<Float>
{

    public FloatMapper()
    {
        super();
    }

    public FloatMapper(int index)
    {
        super(index);
    }

    public FloatMapper(String name)
    {
        super(name);
    }

    @Override
    protected Float extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getFloat(name);
    }

    @Override
    protected Float extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getFloat(index);
    }

    public static final FloatMapper FIRST = new FloatMapper();
}

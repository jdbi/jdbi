package org.skife.jdbi.v2.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DoubleMapper extends TypedMapper<Double>
{
    public DoubleMapper()
    {
        super();
    }

    public DoubleMapper(int index)
    {
        super(index);
    }

    public DoubleMapper(String name)
    {
        super(name);
    }

    @Override
    protected Double extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getDouble(name);
    }

    @Override
    protected Double extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getDouble(index);
    }

    public static final DoubleMapper FIRST = new DoubleMapper();
}

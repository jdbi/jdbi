package org.skife.jdbi.v2.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ByteArrayMapper extends TypedMapper<byte[]>
{

    public ByteArrayMapper()
    {
        super();
    }

    public ByteArrayMapper(int index)
    {
        super(index);
    }

    public ByteArrayMapper(String name)
    {
        super(name);
    }

    @Override
    protected byte[] extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getBytes(name);
    }

    @Override
    protected byte[] extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getBytes(index);
    }

    public static final ByteArrayMapper FIRST = new ByteArrayMapper();
}

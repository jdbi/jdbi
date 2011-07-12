package org.skife.jdbi.v2.util;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ByteMapper extends TypedMapper<Byte>
{

    public ByteMapper()
    {
        super();
    }

    public ByteMapper(int index) {
        super(index);
    }

    public ByteMapper(String name)
    {
        super(name);
    }

    @Override
    protected Byte extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getByte(name);
    }

    @Override
    protected Byte extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getByte(index);
    }

    public static ByteMapper FIRST = new ByteMapper();
}

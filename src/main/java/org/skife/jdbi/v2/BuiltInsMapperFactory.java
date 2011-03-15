package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.skife.jdbi.v2.util.IntegerMapper;
import org.skife.jdbi.v2.util.LongMapper;
import org.skife.jdbi.v2.util.StringMapper;
import org.skife.jdbi.v2.util.TimestampMapper;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

class BuiltInsMapperFactory implements ResultSetMapperFactory
{
    private static final Map<Class, ResultSetMapper> mappers = new HashMap<Class, ResultSetMapper>();

    static {
        mappers.put(int.class, IntegerMapper.FIRST);
        mappers.put(Integer.class, IntegerMapper.FIRST);

        mappers.put(long.class, LongMapper.FIRST);
        mappers.put(Long.class, LongMapper.FIRST);

        mappers.put(Timestamp.class, TimestampMapper.FIRST);

        mappers.put(String.class, StringMapper.FIRST);
    }

    public boolean accepts(Class type, StatementContext ctx)
    {
        return mappers.containsKey(type);
    }

    public ResultSetMapper mapperFor(Class type, StatementContext ctx)
    {
        return mappers.get(type);
    }
}

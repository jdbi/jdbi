package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.PrimitivesMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;

class FigureItOutResultSetMapper implements ResultSetMapper<Object>
{
    private static final PrimitivesMapperFactory factory = new PrimitivesMapperFactory();

    public Object map(int index, ResultSet r, StatementContext ctx) throws SQLException
    {
        Method m = ctx.getSqlObjectMethod();
        m.getAnnotation(GetGeneratedKeys.class);
        Class<?> rt = m.getReturnType();
        ResultSetMapper f = factory.mapperFor(rt, ctx);
        return f.map(index, r, ctx);
    }
}

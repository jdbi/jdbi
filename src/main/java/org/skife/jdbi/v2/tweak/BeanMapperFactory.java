package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.BeanMapper;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;

public class BeanMapperFactory implements ResultSetMapperFactory
{
    @Override
    public boolean accepts(Class type, StatementContext ctx)
    {
        return true;
    }

    @Override
    public ResultSetMapper mapperFor(Class type, StatementContext ctx)
    {
        return new BeanMapper(type);
    }
}

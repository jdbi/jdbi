package org.jdbi.v3.pg;

import org.jdbi.v3.ResultColumnMapperFactory;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.tweak.ResultColumnMapper;

public class SqlArrayMapperFactory implements ResultColumnMapperFactory {

    @Override
    public boolean accepts(Class<?> type, StatementContext ctx) {
        return type.isArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ResultColumnMapper<? extends T> columnMapperFor(Class<T> type, StatementContext ctx) {
        return (ResultColumnMapper<? extends T>) new ArrayColumnMapper(type.getComponentType(), ctx);
    }
}

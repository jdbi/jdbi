package org.jdbi.v3.pg;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.ColumnMapperFactory;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Types;
import org.jdbi.v3.tweak.ColumnMapper;

public class SqlArrayMapperFactory implements ColumnMapperFactory {

    @Override
    public Optional<ColumnMapper<?>> build(Type type, StatementContext ctx) {
        final Class<?> clazz = Types.getErasedType(type);
        return clazz.isArray() ?
                Optional.of(new ArrayColumnMapper(clazz.getComponentType(), ctx)) : Optional.empty();
    }
}

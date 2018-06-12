package org.jdbi.v3.core.mapper;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

public class MapMapperFactory implements RowMapperFactory {
    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        if (type instanceof ParameterizedType && GenericTypes.getErasedType(type) == Map.class) {
            Type valueType = GenericTypes.findGenericParameter(type, Map.class, 1).orElseThrow(IllegalStateException::new);
            Optional<ColumnMapper<?>> colMapper = config.get(ColumnMappers.class).findFor(valueType);

            if (colMapper.isPresent()) {
                ColumnMapper<?> columnMapper = colMapper.get();

                return Optional.of(new MapMapper(columnMapper));
            }
        }

        return Optional.empty();
    }
}

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.core.mapper;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * Maps rows to {@link Map.Entry Map.Entry&lt;K, V&gt;}, provided there are mappers registered for types K and V. This mapper is registered out of the box.
 *
 * @param <K> entry key type
 * @param <V> entry value type
 */
public class MapEntryMapper<K, V> implements RowMapper<Map.Entry<K, V>> {
    private static final TypeVariable<Class<Map.Entry>> KEY_PARAM;
    private static final TypeVariable<Class<Map.Entry>> VALUE_PARAM;

    static {
        TypeVariable<Class<Map.Entry>>[] mapParams = Map.Entry.class.getTypeParameters();
        KEY_PARAM = mapParams[0];
        VALUE_PARAM = mapParams[1];
    }

    private final RowMapper<K> keyMapper;
    private final RowMapper<V> valueMapper;

    @SuppressWarnings("unchecked")
    static RowMapperFactory factory() {
        return (type, config) -> {
            if (type instanceof ParameterizedType && getErasedType(type).equals(Map.Entry.class)) {
                Type keyType = resolveType(KEY_PARAM, type);
                Type valueType = resolveType(VALUE_PARAM, type);

                RowMapper<?> keyMapper = getKeyMapper(keyType, config);
                RowMapper<?> valueMapper = getValueMapper(valueType, config);

                return Optional.of(new MapEntryMapper(keyMapper, valueMapper));
            }
            return Optional.empty();
        };
    }

    private static RowMapper<?> getKeyMapper(Type keyType, ConfigRegistry config) {
        String column = config.get(MapEntryMappers.class).getKeyColumn();
        if (column == null) {
            return config.get(RowMappers.class)
                    .findFor(keyType)
                    .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for map key " + keyType));
        } else {
            return config.get(ColumnMappers.class)
                    .findFor(keyType)
                    .map(mapper -> new SingleColumnMapper<>(mapper, column))
                    .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for map key " + keyType + " in column " + column));
        }
    }

    private static RowMapper<?> getValueMapper(Type valueType, ConfigRegistry config) {
        String column = config.get(MapEntryMappers.class).getValueColumn();
        if (column == null) {
            return config.get(RowMappers.class)
                    .findFor(valueType)
                    .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for map value " + valueType));
        } else {
            return config.get(ColumnMappers.class)
                    .findFor(valueType)
                    .map(mapper -> new SingleColumnMapper<>(mapper, column))
                    .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for map value " + valueType + " in column " + column));
        }
    }

    private MapEntryMapper(RowMapper<K> keyMapper, RowMapper<V> valueMapper) {
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
    }

    @Override
    public Map.Entry<K, V> map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new AbstractMap.SimpleImmutableEntry<>(keyMapper.map(rs, ctx), valueMapper.map(rs, ctx));
    }

    @Override
    public RowMapper<Map.Entry<K, V>> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        return new MapEntryMapper<>(keyMapper.specialize(rs, ctx), valueMapper.specialize(rs, ctx));
    }

}

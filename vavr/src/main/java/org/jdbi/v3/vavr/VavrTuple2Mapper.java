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
package org.jdbi.v3.vavr;

import io.vavr.Tuple2;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.NoSuchMapperException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.SingleColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.generic.GenericTypes.resolveType;

/**
 * provide functionality similar to {@link org.jdbi.v3.core.mapper.MapEntryMapper}
 *
 * since {@link Tuple2} is in fact the Vavr Map Entry, this class is needed to treat it slightly different
 * then other subclasses of {@link io.vavr.Tuple}
 *
 * @param <K>
 * @param <V>
 */
public class VavrTuple2Mapper<K, V> implements RowMapper<Tuple2<K, V>> {
    private static final TypeVariable<Class<Tuple2>> KEY_PARAM;
    private static final TypeVariable<Class<Tuple2>> VALUE_PARAM;
    private static final int TUPLE_VALUE_COLUMN_INDEX = 2;

    static {
        TypeVariable<Class<Tuple2>>[] mapParams = Tuple2.class.getTypeParameters();
        KEY_PARAM = mapParams[0];
        VALUE_PARAM = mapParams[1];
    }

    private final RowMapper<K> keyMapper;
    private final RowMapper<V> valueMapper;

    private VavrTuple2Mapper(RowMapper<K> keyMapper, RowMapper<V> valueMapper) {
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
    }

    @SuppressWarnings("unchecked")
    public static RowMapperFactory factory() {
        return (type, config) -> {
            if (type instanceof ParameterizedType && getErasedType(type).equals(Tuple2.class)) {
                Type keyType = resolveType(KEY_PARAM, type);
                Type valueType = resolveType(VALUE_PARAM, type);

                RowMapper<?> keyMapper = getKeyMapper(keyType, config);
                RowMapper<?> valueMapper = getValueMapper(valueType, config);

                return Optional.of(new VavrTuple2Mapper<>(keyMapper, valueMapper));
            }
            return Optional.empty();
        };
    }

    private static RowMapper<?> getKeyMapper(Type keyType, ConfigRegistry config) {
        String column = config.get(TupleMappers.class).getKeyColumn();
        if (column == null) {
            // precedence of col mappers over row mappers to allow for expected behaviour in tuple2 projection
            return config.get(ColumnMappers.class)
                    .findFor(keyType)
                    .map(mapper -> (RowMapper) new SingleColumnMapper<>(mapper))
                    .orElseGet(() -> config.get(RowMappers.class)
                            .findFor(keyType)
                            .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for map key " + keyType)));
        } else {
            return config.get(ColumnMappers.class)
                    .findFor(keyType)
                    .map(mapper -> new SingleColumnMapper<>(mapper, column))
                    .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for map key " + keyType + " in column " + column));
        }
    }

    private static RowMapper<?> getValueMapper(Type valueType, ConfigRegistry config) {
        String column = config.get(TupleMappers.class).getValueColumn();
        if (column == null) {
            // precedence of col mappers over row mappers to allow for expected behaviour in tuple2 projection
            return config.get(ColumnMappers.class)
                    .findFor(valueType)
                    .map(mapper -> (RowMapper) new SingleColumnMapper<>(mapper, TUPLE_VALUE_COLUMN_INDEX))
                    .orElseGet(() -> config.get(RowMappers.class)
                            .findFor(valueType)
                            .orElseThrow(() -> new NoSuchMapperException("No row mapper registered for map value " + valueType)));
        } else {
            return config.get(ColumnMappers.class)
                    .findFor(valueType)
                    .map(mapper -> new SingleColumnMapper<>(mapper, column))
                    .orElseThrow(() -> new NoSuchMapperException("No column mapper registered for map value " + valueType + " in column " + column));
        }
    }

    @Override
    public Tuple2<K, V> map(ResultSet rs, StatementContext ctx) throws SQLException {
        return new Tuple2<>(keyMapper.map(rs, ctx), valueMapper.map(rs, ctx));
    }

    @Override
    public RowMapper<Tuple2<K, V>> specialize(ResultSet rs, StatementContext ctx) throws SQLException {
        return new VavrTuple2Mapper<>(keyMapper.specialize(rs, ctx), valueMapper.specialize(rs, ctx));
    }

}

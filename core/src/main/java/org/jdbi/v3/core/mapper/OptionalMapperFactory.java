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

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map JDBC-recognized types, along with some other well-known types
 * from the JDK.
 */
public class OptionalMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, ColumnMapper<?>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put(OptionalInt.class, optionalMapper(ResultSet::getInt, OptionalInt::empty, OptionalInt::of));
        MAPPERS.put(OptionalLong.class, optionalMapper(ResultSet::getLong, OptionalLong::empty, OptionalLong::of));
        MAPPERS.put(OptionalDouble.class, optionalMapper(ResultSet::getDouble, OptionalDouble::empty, OptionalDouble::of));
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> rawType = getErasedType(type);

        if (rawType == Optional.class) {
            return Optional.of(OptionalMapper.of(type));
        }

        return Optional.ofNullable(MAPPERS.get(rawType));
    }

    private static <T> ColumnMapper<T> referenceMapper(ColumnGetter<T> getter) {
        return (r, i, ctx) -> {
            T value = getter.get(r, i);
            return r.wasNull() ? null : value;
        };
    }

    private static <Opt, Box> ColumnMapper<?> optionalMapper(ColumnGetter<Box> columnGetter, Supplier<Opt> empty, Function<Box, Opt> present) {
        return (ColumnMapper<Opt>) (r, columnNumber, ctx) -> {
            final Box boxed = referenceMapper(columnGetter).map(r, columnNumber, ctx);
            if (boxed == null) {
                return empty.get();
            }
            return present.apply(boxed);
        };
    }
}

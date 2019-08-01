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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Column mapper factory which knows how to map Optionals:
 * <ul>
 *     <li>{@link Optional}</li>
 *     <li>{@link OptionalInt}</li>
 *     <li>{@link OptionalLong}</li>
 *     <li>{@link OptionalDouble}</li>
 * </ul>
 */
class OptionalMapperFactory implements ColumnMapperFactory {
    private static final Map<Class<?>, BiFunction<Type, ConfigRegistry, ColumnMapper<?>>> STRATEGIES;

    static {
        Map<Class<?>, BiFunction<Type, ConfigRegistry, ColumnMapper<?>>> s = new HashMap<>();

        s.put(Optional.class, OptionalMapperFactory::create);
        s.put(OptionalInt.class, singleton(create(ResultSet::getInt, OptionalInt::empty, OptionalInt::of)));
        s.put(OptionalLong.class, singleton(create(ResultSet::getLong, OptionalLong::empty, OptionalLong::of)));
        s.put(OptionalDouble.class, singleton(create(ResultSet::getDouble, OptionalDouble::empty, OptionalDouble::of)));

        STRATEGIES = Collections.unmodifiableMap(s);
    }

    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return Optional.ofNullable(STRATEGIES.get(getErasedType(type)))
                .map(strategy -> strategy.apply(type, config));
    }

    static BiFunction<Type, ConfigRegistry, ColumnMapper<?>> singleton(ColumnMapper<?> instance) {
        return (t, c) -> instance;
    }

    static <Opt, Box> ColumnMapper<?> create(ColumnGetter<Box> columnGetter, Supplier<Opt> empty, Function<Box, Opt> present) {
        return (r, columnNumber, ctx) -> Optional.ofNullable(new GetterMapper<>(columnGetter).map(r, columnNumber, ctx))
                .map(present)
                .orElseGet(empty);
    }

    private static ColumnMapper<?> create(Type type, ConfigRegistry config) {
        final ColumnMapper<?> mapper = config.get(ColumnMappers.class).findFor(
                GenericTypes.findGenericParameter(type, Optional.class)
                    .orElseThrow(() -> new NoSuchMapperException("No mapper for raw Optional type")))
                .orElseThrow(() -> new NoSuchMapperException("No mapper for type " + type + " nested in Optional"));
        return (r, i, ctx) -> (Optional<?>) Optional.ofNullable(mapper.map(r, i, ctx));
    }
}

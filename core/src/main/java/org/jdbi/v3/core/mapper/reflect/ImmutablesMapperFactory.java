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
package org.jdbi.v3.core.mapper.reflect;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.internal.ImmutablesPropertiesFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;
import org.jdbi.v3.meta.Beta;

@Beta
public class ImmutablesMapperFactory<T> implements RowMapperFactory {

    private final Class<T> defn;
    private final Class<? extends T> impl;
    private final Function<Type, PojoProperties<T>> properties;

    private ImmutablesMapperFactory(Class<T> defn, Class<? extends T> impl, Function<Type, PojoProperties<T>> properties) {
        this.defn = defn;
        this.impl = impl;
        this.properties = properties;
    }

    public static <T, I extends T, B> RowMapperFactory mapImmutable(Class<T> defn, Class<I> immutable, Supplier<B> builder) {
        return new ImmutablesMapperFactory<>(defn, immutable, ImmutablesPropertiesFactory.immutable(defn, builder));
    }

    public static <T, M extends T> RowMapperFactory mapModifiable(Class<T> defn, Class<M> modifiable, Supplier<M> constructor) {
        return new ImmutablesMapperFactory<>(defn, modifiable, ImmutablesPropertiesFactory.modifiable(defn, constructor));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> erasedType = GenericTypes.getErasedType(type);
        if (defn.equals(erasedType) || impl.equals(erasedType)) {
            return Optional.of(PropertiesMapper.of((Class<T>) erasedType, properties.apply(type)));
        }
        return Optional.empty();
    }
}

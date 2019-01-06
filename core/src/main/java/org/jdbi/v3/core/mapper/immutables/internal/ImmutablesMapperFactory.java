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
package org.jdbi.v3.core.mapper.immutables.internal;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.mapper.reflect.internal.PojoMapper;
import org.jdbi.v3.core.mapper.reflect.internal.PojoProperties;

/**
 * Row mapper that inspects an {@code immutables}-style Immutable or Modifiable value class for properties
 * and binds them in the style of {@link org.jdbi.v3.core.mapper.reflect.BeanMapper}.
 * @param <T> the mapped value type
 */
public class ImmutablesMapperFactory<T> implements RowMapperFactory {

    private final Class<T> defn;
    private final Class<? extends T> impl;
    private final Function<Type, ? extends PojoProperties<T>> properties;

    public ImmutablesMapperFactory(Class<T> defn, Class<? extends T> impl, Function<Type, ? extends PojoProperties<T>> properties) {
        this.defn = defn;
        this.impl = impl;
        this.properties = properties;
    }

    @Override
    public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {
        Class<?> erasedType = GenericTypes.getErasedType(type);
        if (defn.equals(erasedType) || impl.equals(erasedType)) {
            return Optional.of(new PojoMapper<>(defn, properties.apply(type), ""));
        }
        return Optional.empty();
    }
}

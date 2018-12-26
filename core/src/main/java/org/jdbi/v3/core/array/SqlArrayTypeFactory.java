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
package org.jdbi.v3.core.array;

import java.lang.reflect.Type;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

/**
 * Factory interface to produce {@link SqlArrayType} instances.
 */
@FunctionalInterface
public interface SqlArrayTypeFactory {
    /**
     * Returns an {@link SqlArrayType} for the given {@code elementType} if this factory supports it; empty otherwise.
     *
     * @param elementType the array element type
     * @param config the config registry, for composition
     * @return an {@link SqlArrayType} for the given {@code elementType} if this factory supports it; empty otherwise.
     * @see SqlArrayTypes#findFor(Type)
     */
    Optional<SqlArrayType<?>> build(Type elementType, ConfigRegistry config);

    /**
     * Create a SqlArrayTypeFactory for the given {@code elementType} that binds using a
     * type name and convert function.
     * @param elementType the array element type
     * @param typeName the database type name to bind as
     * @param conversion the function to convert to jdbc representation
     * @return the built factory
     * @see SqlArrayType#of(String, Function)
     */
    static <T> SqlArrayTypeFactory of(Class<T> elementType, String typeName, Function<T, ?> conversion) {
        final Optional<SqlArrayType<?>> arrayType = Optional.of(SqlArrayType.of(typeName, conversion));
        return (t, c) -> elementType == GenericTypes.getErasedType(t) ? arrayType : Optional.empty();
    }
}

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
import java.sql.Types;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

public class SqlArrayArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        Class<?> erasedType = GenericTypes.getErasedType(type);

        if (!(erasedType.isArray() || Collection.class.isAssignableFrom(erasedType))) {
            return Optional.empty();
        }

        if (value == null) {
            return Optional.of(new NullArgument(Types.ARRAY));
        }

        Function<Type, Optional<SqlArrayType<?>>> lookup =
                eT -> config.get(SqlArrayTypes.class).findFor(eT);

        if (erasedType.isArray()) {
            Class<?> elementType = erasedType.getComponentType();
            return lookup.apply(elementType)
                    .map(arrayType -> new SqlArrayArgument<>(arrayType, value));
        }

        final Collection<?> collection = (Collection<?>) value;
        // If there's no static type, guess based on the first element
        final Type eltType = GenericTypes.findGenericParameter(type, Collection.class)
            .orElse(collection.isEmpty() ? String.class : collection.iterator().next().getClass());

        return lookup.apply(eltType)
                .map(arrayType -> new SqlArrayArgument<>(arrayType, (Collection<?>) value));
    }
}

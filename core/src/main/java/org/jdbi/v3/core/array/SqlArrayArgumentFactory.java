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
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;

/**
 * Bind a Java array or {@link Collection} to a SQL array using the
 * {@link SqlArrayTypeFactory} for the given type information.  Note
 * that due to type erasure, binding {@code Collection} arguments with
 * {@link org.jdbi.v3.core.statement.SqlStatement#bind(int, Object)} may
 * fail to determine the array component type.  Provide explicit information with
 * {@link org.jdbi.v3.core.statement.SqlStatement#bindByType(int, Object, org.jdbi.v3.core.generic.GenericType)}.
 *
 * This factory is registered by default.
 * @see SqlArrayType
 * @see SqlArrayTypes
 * @see org.jdbi.v3.core.config.Configurable#registerArrayType(SqlArrayType)
 */
public class SqlArrayArgumentFactory implements ArgumentFactory {
    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        Class<?> rawClass = GenericTypes.getErasedType(type);

        if (!rawClass.isArray() && !Collection.class.isAssignableFrom(rawClass)) {
            return Optional.empty();
        }

        if (value == null) {
            return Optional.of(new NullArgument(Types.ARRAY));
        }

        if (rawClass.isArray()) {
            return config.get(SqlArrayTypes.class)
                .findFor(rawClass.getComponentType())
                .map(arrayType -> new SqlArrayArgument<>(arrayType, value));
        } else {
            return GenericTypes.findGenericParameter(type, Collection.class)
                .flatMap(config.get(SqlArrayTypes.class)::findFor)
                .map(arrayType -> new SqlArrayArgument<>(arrayType, value));
        }
    }
}

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
package org.jdbi.v3.postgres;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.util.GenericTypes;

import java.lang.reflect.Type;
import java.util.EnumSet;
import java.util.Optional;

public class EnumSetArgumentFactory implements ArgumentFactory {

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        Class<?> erasedType = GenericTypes.getErasedType(type);
        if (EnumSet.class.isAssignableFrom(erasedType)) {
            Optional<Type> genericParameter = GenericTypes.findGenericParameter(type, EnumSet.class);
            Class<Enum<?>> enumType = (Class<Enum<?>>) genericParameter
                    .orElseThrow(() -> new IllegalArgumentException("No generic type information for " + type));
            return Optional.of(new EnumSetArgument(enumType, (EnumSet<?>) value));
        }
        return Optional.empty();
    }
}

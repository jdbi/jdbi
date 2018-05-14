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
package org.jdbi.v3.guava;

import java.lang.reflect.Type;
import java.util.Optional;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

/**
 * Provide ArgumentFactory instances that understand Guava types.
 */
public class GuavaArguments {
    private GuavaArguments() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Returns an {@link ArgumentFactory} which understands Guava types.
     *
     * <p>Currently supported types:</p>
     * <ul>
     * <li>{@code com.google.common.base.Optional}</li>
     * </ul>
     *
     * @return an {@link ArgumentFactory} which understands Guava types.
     */
    public static ArgumentFactory factory() {
        return new Factory();
    }

    private static class Factory implements ArgumentFactory {

        @Override
        public Optional<Argument> build(Type expectedType, Object value, ConfigRegistry config) {
            if (value instanceof com.google.common.base.Optional) {
                Object nestedValue = ((com.google.common.base.Optional<?>) value).orNull();
                Type nestedType = findOptionalType(expectedType, nestedValue);
                return config.get(Arguments.class).findFor(nestedType, nestedValue);
            }

            return Optional.empty();
        }

        private Type findOptionalType(Type wrapperType, Object nestedValue) {
            if (getErasedType(wrapperType).equals(com.google.common.base.Optional.class)) {
                Optional<Type> nestedType = findGenericParameter(wrapperType, com.google.common.base.Optional.class);
                if (nestedType.isPresent()) {
                    return nestedType.get();
                }
            }
            return nestedValue == null ? Object.class : nestedValue.getClass();
        }

    }
}

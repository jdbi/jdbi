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

import static org.jdbi.v3.core.util.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.util.GenericTypes.getErasedType;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.NullValue;

/**
 * Provide ArgumentFactory instances that understand Guava types.
 */
public class GuavaArguments {
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

        @SuppressWarnings("unchecked")
        @Override
        public Optional<Argument> build(Type expectedType, ConfigRegistry config) {
            if (com.google.common.base.Optional.class.isAssignableFrom(getErasedType(expectedType))) {
                Optional<Argument> argument = findGenericParameter(expectedType, com.google.common.base.Optional.class)
                        .flatMap(config::findArgumentFor)
                        .map(this::applyOPtional);

                return argument.isPresent()
                        ? argument
                        : Optional.of(bestEffortOptionalArgument());
            }

            return Optional.empty();
        }

        private <T> Argument<com.google.common.base.Optional<T>> applyOPtional(Argument<T> delegate) {
            return (s, p, value, ctx) -> delegate.apply(s, p, value.orNull(), ctx);
        }

        @SuppressWarnings("unchecked")
        private Argument<com.google.common.base.Optional> bestEffortOptionalArgument() {
            // no static type information -- best effort based on runtime type information
            return (stmt, pos, value, ctx) -> {
                Object nested = value.or(NullValue::new);
                ctx.findArgumentFor(nested.getClass())
                        .orElseThrow(() -> new UnsupportedOperationException(
                                "No argument registered for value " + nested + " of type " + nested.getClass()))
                        .apply(stmt, pos, nested, ctx);
            };
        }
    }
}

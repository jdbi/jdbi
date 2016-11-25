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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.StatementContext;
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

    private static class OptionalArgument<T> implements Argument<com.google.common.base.Optional<T>> {
        private final Argument<T> delegate;

        OptionalArgument(Argument<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void apply(PreparedStatement statement,
                          int position,
                          com.google.common.base.Optional<T> value,
                          StatementContext ctx) throws SQLException {
            delegate.apply(statement, position, value.orNull(), ctx);
        }
    }

    @SuppressWarnings("unchecked")
    private static class BestEffortOptionalArgument implements Argument<com.google.common.base.Optional> {
        @Override
        public void apply(PreparedStatement statement,
                          int position,
                          com.google.common.base.Optional value,
                          StatementContext ctx) throws SQLException {
            Object nested = value.or(NullValue::new);
            Argument<Object> argument = (Argument<Object>) ctx.findArgumentFor(nested.getClass())
                    .orElseThrow(() -> new UnsupportedOperationException(
                            "No argument registered for value " + nested + " of type " + nested.getClass()));
            argument.apply(statement, position, nested, ctx);
        }
    }

    private static class Factory implements ArgumentFactory {

        @SuppressWarnings("unchecked")
        @Override
        public Optional<Argument<?>> build(Type expectedType, ConfigRegistry config) {
            if (com.google.common.base.Optional.class.isAssignableFrom(getErasedType(expectedType))) {
                Optional<Argument<?>> argument = findGenericParameter(expectedType, com.google.common.base.Optional.class)
                        .flatMap(config::findArgumentFor)
                        .map(OptionalArgument::new);

                return argument.isPresent()
                        ? argument
                        : Optional.of(new BestEffortOptionalArgument());
            }

            return Optional.empty();
        }
    }
}

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
package org.jdbi.core.argument;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.statement.UnableToCreateStatementException;

import static org.jdbi.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.core.generic.GenericTypes.getErasedType;
import static org.jdbi.core.generic.GenericTypes.isSuperType;

/**
 * An {@link ArgumentFactory} base class for arguments of type {@code T}. For values of type {@code T}, factories
 * produces arguments from the {@link #build(Object, ConfigRegistry)} method. For null values with a known expected type
 * of {@code T}, produces null arguments for the {@code sqlType} passed to the constructor.
 * <pre>
 * class ValueType {
 *     String value;
 * }
 *
 * class ValueTypeArgumentFactory extends AbstractArgumentFactory&lt;ValueType&gt; {
 *     ValueTypeArgumentFactory() {
 *         super(Types.VARCHAR);
 *     }
 *
 *     &#64;Override
 *     protected Argument build(ValueType valueType, ConfigRegistry config) {
 *         return (pos, stmt, ctx) -&gt; stmt.setString(pos, valueType.value);
 *     }
 * }
 * </pre>
 *
 * Don't forget to override {@link Object#toString} in your {@link Argument} instances if you want to be able to log their values with an {@link org.jdbi.core.statement.SqlLogger}.
 *
 * @param <T> the type of argument supported by this factory.
 */
public abstract class AbstractArgumentFactory<T> implements ArgumentFactory.Preparable {
    private final int sqlType;
    private final ArgumentPredicate isInstance;
    private final Type argumentType;

    /**
     * Constructs an {@link ArgumentFactory} for type {@code T}.
     *
     * @param sqlType the {@link java.sql.Types} constant to use when the argument value is {@code null}.
     */
    protected AbstractArgumentFactory(int sqlType) {
        this.sqlType = sqlType;
        argumentType = findGenericParameter(getClass(), AbstractArgumentFactory.class)
                .orElseThrow(() -> new IllegalStateException(getClass().getSimpleName()
                    + " must extend AbstractArgumentFactory with a concrete T parameter"));

        if (argumentType instanceof Class<?> argumentClass) {
            this.isInstance = (type, value) ->
                    argumentClass.isAssignableFrom(getErasedType(type)) || argumentClass.isInstance(value);
        } else {
            this.isInstance = (type, value) ->
                argumentType.equals(type) || isSuperType(argumentType, type);
        }
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        return isInstance.test(type, null)
                ? Optional.of(value -> innerBuild(type, value, config))
                : Optional.empty();
    }

    /**
     * @deprecated no longer used
     */
    @Override
    @Deprecated(since = "3.39.0", forRemoval = true)
    public Collection<Type> prePreparedTypes() {
        return Collections.singletonList(argumentType);
    }

    @Override
    public final Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        return isInstance.test(type, value)
                ? Optional.of(innerBuild(type, value, config))
                : Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private Argument innerBuild(Type type, Object value, ConfigRegistry config) {
        if (value == null) {
            return new NullArgument(sqlType);
        }

        Argument argument = build((T) value, config);

        if (argument == null) {
            throw new UnableToCreateStatementException("Prepared argument " + value + " of type " + type + " failed to build");
        }

        return argument;
    }

    /**
     * Produce an argument object for the given value. When the implementation class has accepted a given
     * type, it must then produce an argument instance or throw an exception.
     *
     * @param value  the value to convert to an argument
     * @param config the config registry
     * @return An {@link Argument} for the given {@code value}. Must not be null!
     */
    protected abstract Argument build(T value, ConfigRegistry config);

    @FunctionalInterface
    private interface ArgumentPredicate {
        boolean test(Type type, Object value);
    }
}

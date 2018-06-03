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
package org.jdbi.v3.core.argument;

import org.jdbi.v3.core.config.ConfigRegistry;

import java.lang.reflect.Type;
import java.util.Optional;

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;

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
 * Don't forget to override {@link Object#toString} in your {@link Argument} instances if you want to be able to log their values with an {@link org.jdbi.v3.core.statement.SqlLogger}.
 *
 * @param <T> the type of argument supported by this factory.
 */
public abstract class AbstractArgumentFactory<T> implements ArgumentFactory {
    private final int sqlType;
    private final ArgumentPredicate isInstance;

    /**
     * Constructs an {@link ArgumentFactory} for type {@code T}.
     *
     * @param sqlType the {@link java.sql.Types} constant to use when the argument value is {@code null}.
     */
    protected AbstractArgumentFactory(int sqlType) {
        this.sqlType = sqlType;
        Type argumentType = findGenericParameter(getClass(), AbstractArgumentFactory.class)
                .orElseThrow(() -> new IllegalStateException(getClass().getSimpleName()
                    + " must extend AbstractArgumentFactory with a concrete T parameter"));

        if (argumentType instanceof Class) {
            Class<?> argumentClass = (Class<?>) argumentType;
            this.isInstance = (type, value) ->
                    argumentClass.isAssignableFrom(getErasedType(type)) || argumentClass.isInstance(value);
        } else {
            this.isInstance = (type, value) -> argumentType.equals(type);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        if (!isInstance.test(type, value)) {
            return Optional.empty();
        }
        return Optional.of(value == null
                ? new NullArgument(sqlType)
                : build((T) value, config));
    }

    /**
     * Produce an argument object for the given value.
     *
     * @param value  the value to convert to an argument
     * @param config the config registry
     * @return an {@link Argument} for the given {@code value}.
     */
    protected abstract Argument build(T value, ConfigRegistry config);

    private interface ArgumentPredicate {
        boolean test(Type type, Object value);
    }
}

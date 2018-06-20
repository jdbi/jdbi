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

import static org.jdbi.v3.core.generic.GenericTypes.findGenericParameter;
import static org.jdbi.v3.core.generic.GenericTypes.findSuperclassAnnotatedTypeParameter;
import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifyingAnnotations;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;

/**
 * A {@link QualifiedArgumentFactory} base class for arguments of qualified type {@code T}. For values of type
 * {@code T}, factories produces arguments from the {@link #build(Object, ConfigRegistry)} method. For null values with
 * a known expected type of {@code T}, produces null arguments for the {@code sqlType} passed to the constructor.
 * <p>
 * Type qualifiers may be specified either by annotating the factory class, annotating the generic {@code <T>} parameter
 * of the factory with one or more qualifying annotation. Alternatively, qualifiers may be provided in the constructor.
 *
 * <pre>
 * interface &#064;ValueType {}
 *
 * // Annotate factory class
 * &#064;ValueType
 * class ValueTypeArgumentFactory extends AbstractArgumentFactory&lt;String&gt; {
 *     ValueTypeArgumentFactory() {
 *         super(Types.VARCHAR);
 *     }
 *
 *     &#64;Override
 *     protected Argument build(String value, ConfigRegistry config) {
 *         return (pos, stmt, ctx) -&gt; stmt.setObject(pos, new SomeVendorObject(value));
 *     }
 * }
 *
 * // Annotate type parameter
 * class ValueTypeArgumentFactory extends AbstractArgumentFactory&lt;&#064;ValueType String&gt; {
 *     ValueTypeArgumentFactory() {
 *         super(Types.VARCHAR);
 *     }
 *
 *     &#64;Override
 *     protected Argument build(String value, ConfigRegistry config) {
 *         return (pos, stmt, ctx) -&gt; stmt.setObject(pos, new SomeVendorObject(value));
 *     }
 * }
 *
 * // Provide type qualifier in constructor
 * class ValueTypeArgumentFactory extends AbstractArgumentFactory&lt;String&gt; {
 *
 *     ValueTypeArgumentFactory() {
 *         super(Types.VARCHAR, MyQualifiers.VALUE_TYPE);
 *     }
 *
 *     &#64;Override
 *     protected Argument build(String value, ConfigRegistry config) {
 *         return (pos, stmt, ctx) -&gt; stmt.setObject(pos, new SomeVendorObject(value));
 *     }
 * }
 * </pre>
 *
 * @param <T> the type of argument supported by this factory.
 */
@Beta
public abstract class AbstractQualifiedArgumentFactory<T> implements QualifiedArgumentFactory {
    private final int sqlType;
    private final ArgumentPredicate isInstance;
    private final Set<?> qualifiers;

    /**
     * Constructs an {@link ArgumentFactory} for type {@code T}.
     *
     * @param sqlType the {@link java.sql.Types} constant to use when the argument value is {@code null}.
     */
    protected AbstractQualifiedArgumentFactory(int sqlType) {
        this.sqlType = sqlType;
        this.isInstance = instanceTest(getClass());
        this.qualifiers = checkQualifiers(getClass(), getQualifyingAnnotations(
            getClass(),
            findSuperclassAnnotatedTypeParameter(getClass(), 0).orElse(null)));
    }

    /**
     * Constructs an {@link ArgumentFactory} for type {@code T}.
     *
     * @param sqlType the {@link java.sql.Types} constant to use when the argument value is {@code null}.
     */
    protected AbstractQualifiedArgumentFactory(int sqlType, Object... qualifiers) {
        this.sqlType = sqlType;
        this.isInstance = instanceTest(getClass());
        this.qualifiers = checkQualifiers(getClass(), new HashSet<>(Arrays.asList(qualifiers)));
    }

    private static ArgumentPredicate instanceTest(Class<? extends AbstractQualifiedArgumentFactory> clazz) {
        Type argumentType = findGenericParameter(clazz, AbstractQualifiedArgumentFactory.class)
            .orElseThrow(() -> new IllegalStateException(clazz.getSimpleName()
                + " must extend AbstractArgumentFactory with a concrete T parameter"));

        if (argumentType instanceof Class) {
            Class<?> argumentClass = (Class<?>) argumentType;
            return (type, value) ->
                argumentClass.isAssignableFrom(getErasedType(type)) || argumentClass.isInstance(value);
        } else {
            return (type, value) -> argumentType.equals(type);
        }
    }

    private static Set<?> checkQualifiers(Class<? extends AbstractQualifiedArgumentFactory> clazz, Set<?> qualifiers) {
        if (qualifiers.isEmpty()) {
            throw new IllegalStateException("Missing type qualifiers for factory class " + clazz.getName()
                + ". Qualifiers may be provided as annotations on the class or on the superclass generic parameter, "
                + "or may be passed explicitly to the superclass constructor.");
        }
        return qualifiers;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final Optional<Argument> build(QualifiedType type, Object value, ConfigRegistry config) {
        if (qualifiers.equals(type.getQualifiers()) && isInstance.test(type.getType(), value)) {
            return Optional.of(value == null
                ? new NullArgument(sqlType)
                : build((T) value, config));
        }
        return Optional.empty();
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

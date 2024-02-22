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
package org.jdbi.core.statement;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.core.Handle;
import org.jdbi.core.argument.Argument;
import org.jdbi.core.argument.BeanPropertyArguments;
import org.jdbi.core.argument.CharacterStreamArgument;
import org.jdbi.core.argument.InputStreamArgument;
import org.jdbi.core.argument.NamedArgumentFinder;
import org.jdbi.core.argument.NullArgument;
import org.jdbi.core.argument.ObjectArgument;
import org.jdbi.core.argument.ObjectFieldArguments;
import org.jdbi.core.argument.ObjectMethodArguments;
import org.jdbi.core.argument.internal.NamedArgumentFinderFactory;
import org.jdbi.core.argument.internal.PojoPropertyArguments;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.internal.IterableLike;
import org.jdbi.core.mapper.immutables.JdbiImmutables;
import org.jdbi.core.qualifier.NVarchar;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.meta.Beta;

import static org.jdbi.core.generic.GenericTypes.arrayType;
import static org.jdbi.core.generic.GenericTypes.parameterizeClass;

@SuppressWarnings("deprecation")
public interface BindingsMixin<This> {
    @SuppressWarnings("unchecked")
    private This typedThis() {
        return (This) this;
    }

    private This define(final String key, final Object value) { throw new UnsupportedOperationException(); }
    private ConfigRegistry getConfig() { throw new UnsupportedOperationException(); }
    Binding getBinding();

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param position position to bindBinaryStream this argument, starting at 0
     * @param argument exotic argument factory
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Argument argument) {
        getBinding().addPositional(position, argument);
        return typedThis();
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param name     name to bindBinaryStream this argument
     * @param argument exotic argument factory
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Argument argument) {
        getBinding().addNamed(name, argument);
        return typedThis();
    }

    /**
     * Binds named parameters from JavaBean properties on the argument.
     *
     * @param bean source of named parameter values to use as arguments
     *
     * @return modified statement
     */
    default This bindBean(final Object bean) {
        return bindBean(null, bean);
    }

    /**
     * Binds named parameters from JavaBean properties on the bean argument, with the given prefix.
     *
     * Example: the prefix {@code foo} applied to a bean property {@code bar} will be bound as {@code foo.bar}.
     *
     * @param prefix a prefix to apply to all property names.
     * @param bean source of named parameter values to use as arguments. Can be null, in this case, nothing is bound.
     *
     * @return modified statement
     */
    default This bindBean(final String prefix, final Object bean) {
        if (bean != null) {
            return bindNamedArgumentFinder(
                NamedArgumentFinderFactory.BEAN,
                prefix,
                bean,
                bean.getClass(),
                () -> new BeanPropertyArguments(prefix, bean, getConfig()));
        }
        return typedThis();
    }

    /**
     * Binds named parameters from object properties on the argument.
     * The type must have been registered with pojo type mapping functionality first, usually
     * by a plugin or configuration.
     *
     * @param pojo source of named parameter values to use as arguments
     *
     * @return modified statement
     * @see JdbiImmutables an example method of registering a type
     */
    @Beta
    default This bindPojo(final Object pojo) {
        return bindPojo(null, pojo);
    }

    /**
     * Binds named parameters from object properties on the bean argument, with the given prefix.
     * The type must have been registered with pojo type mapping functionality first, usually
     * by a plugin or configuration.
     *
     * @param prefix a prefix to apply to all property names.
     * @param pojo source of named parameter values to use as arguments
     *
     * @return modified statement
     * @see JdbiImmutables an example method of registering a type
     */
    @Beta
    default This bindPojo(final String prefix, final Object pojo) {
        return bindPojo(prefix, pojo, pojo.getClass());
    }

    /**
     * Binds named parameters from object properties on the argument.
     * The type must have been registered with pojo type mapping functionality first, usually
     * by a plugin or configuration.
     *
     * @param pojo source of named parameter values to use as arguments
     * @param type the static, possibly generic type of the pojo
     *
     * @return modified statement
     * @see JdbiImmutables an example method of registering a type
     */
    @Beta
    default This bindPojo(final Object pojo, final Type type) {
        return bindPojo(null, pojo, type);
    }

    /**
     * Binds named parameters from object properties on the bean argument, with the given prefix.
     * The type must have been registered with pojo type mapping functionality first, usually
     * by a plugin or configuration.
     *
     * @param prefix a prefix to apply to all property names.
     * @param pojo source of named parameter values to use as arguments
     * @param type the static, possibly generic type of the pojo
     *
     * @return modified statement
     * @see JdbiImmutables an example method of registering a type
     */
    @Beta
    default This bindPojo(final String prefix, final Object pojo, final Type type) {
        if (pojo != null) {
            return bindNamedArgumentFinder(
                NamedArgumentFinderFactory.POJO,
                prefix,
                pojo,
                type,
                () -> new PojoPropertyArguments(prefix, pojo, type, getConfig()));
        }
        return typedThis();
    }

    /**
     * Binds named parameters from object properties on the argument.
     * The type must have been registered with pojo type mapping functionality first, usually
     * by a plugin or configuration.
     *
     * @param pojo source of named parameter values to use as arguments
     * @param type the static generic type of the pojo
     *
     * @return modified statement
     * @see JdbiImmutables an example method of registering a type
     */
    @Beta
    default This bindPojo(final Object pojo, final GenericType<?> type) {
        return bindPojo(null, pojo, type.getType());
    }

    /**
     * Binds named parameters from object properties on the bean argument, with the given prefix.
     * The type must have been registered with pojo type mapping functionality first, usually
     * by a plugin or configuration.
     *
     * @param prefix a prefix to apply to all property names.
     * @param pojo source of named parameter values to use as arguments
     * @param type the static generic type of the pojo
     *
     * @return modified statement
     * @see JdbiImmutables an example method of registering a type
     */
    @Beta
    default This bindPojo(final String prefix, final Object pojo, final GenericType<?> type) {
        return bindPojo(prefix, pojo, type.getType());
    }

    /**
     * Binds public fields of the specified object as arguments for the query.
     *
     * @param object source of the public fields to bind.
     *
     * @return modified statement
     */
    default This bindFields(final Object object) {
        return bindFields(null, object);
    }

    /**
     * Binds public fields of the specified object as arguments for the query.
     *
     * @param prefix a prefix to apply to all field names.
     * @param object source of the public fields to bind. If the object is null, nothing is bound.
     *
     * @return modified statement
     */
    default This bindFields(final String prefix, final Object object) {
        if (object != null) {
            return bindNamedArgumentFinder(
                NamedArgumentFinderFactory.FIELDS,
                prefix,
                object,
                object.getClass(),
                () -> new ObjectFieldArguments(prefix, object));
        }
        return typedThis();
    }

    /**
     * Binds methods with no parameters on the argument.
     *
     * @param object source of methods to use as arguments
     *
     * @return modified statement
     */
    default This bindMethods(final Object object) {
        return bindMethods(null, object);
    }

    /**
     * Binds methods with no parameters on the argument, with the given prefix.
     *
     * @param prefix a prefix to apply to all property names.
     * @param object source of methods to use as arguments. If the object is null, nothing is bound.
     *
     * @return modified statement
     */
    default This bindMethods(final String prefix, final Object object) {
        if (object != null) {
            return bindNamedArgumentFinder(
                NamedArgumentFinderFactory.METHODS,
                prefix,
                object,
                object.getClass(),
                () -> new ObjectMethodArguments(prefix, object));
        }
        return typedThis();
    }

    /**
     * Binds named parameters from a map of String to Object instances
     *
     * @param map map where keys are matched to named parameters in order to bind arguments.
     *            Can be null, in which case the binding has no effect.
     *
     * @return modified statement
     */
    default This bindMap(final Map<String, ?> map) {
        if (map != null) {
            map.forEach(this::bind);
        }
        return typedThis();
    }

    /**
     * Binds a new {@link NamedArgumentFinder}.
     *
     * @param namedArgumentFinder A NamedArgumentFinder to bind. Can be null.
     *
     * @return the same Query instance
     */
    default This bindNamedArgumentFinder(final NamedArgumentFinder namedArgumentFinder) {
        if (namedArgumentFinder != null) {
            getBinding().addNamedArgumentFinder(namedArgumentFinder);
        }

        return typedThis();
    }

    default This bindNamedArgumentFinder(
            final NamedArgumentFinderFactory factory,
            final String prefix,
            final Object value,
            final Type type,
            final Supplier<NamedArgumentFinder> namedArgumentFinder) {
        return bindNamedArgumentFinder(namedArgumentFinder.get());
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Character value) {
        return bind(position, Character.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Character value) {
        return bind(name, Character.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final String value) {
        return bind(position, String.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final String value) {
        return bind(name, String.class, value);
    }

    /**
     * Bind a {@code String} argument positionally, as {@code NVARCHAR} type.
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bindNVarchar(final int position, final String value) {
        return bindByType(position, value, QualifiedType.of(String.class).with(NVarchar.class));
    }

    /**
     * Bind a {@code String} argument by name, as {@code NVARCHAR} type.
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bindNVarchar(final String name, final String value) {
        return bindByType(name, value, QualifiedType.of(String.class).with(NVarchar.class));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final int value) {
        return bind(position, int.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Integer value) {
        return bind(position, Integer.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final int value) {
        return bind(name, int.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Integer value) {
        return bind(name, Integer.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final char value) {
        return bind(position, char.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final char value) {
        return bind(name, char.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     * @param length   how long is the stream being bound?
     *
     * @return the same Query instance
     */
    default This bindASCIIStream(final int position, final InputStream value, final int length) {
        return bind(position, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bind the parameter to
     * @param value  to bind
     * @param length bytes to read from value
     *
     * @return the same Query instance
     */
    default This bindASCIIStream(final String name, final InputStream value, final int length) {
        return bind(name, new InputStreamArgument(value, length, true));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final BigDecimal value) {
        return bind(position, BigDecimal.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final BigDecimal value) {
        return bind(name, BigDecimal.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     * @param length the number of bytes in the stream.
     *
     * @return the same Query instance
     */
    default This bindBinaryStream(final int position, final InputStream value, final int length) {
        return bind(position, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bind the parameter to
     * @param value  to bind
     * @param length bytes to read from value
     *
     * @return the same Query instance
     */
    default This bindBinaryStream(final String name, final InputStream value, final int length) {
        return bind(name, new InputStreamArgument(value, length, false));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Blob value) {
        return bind(position, Blob.class, value);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Blob value) {
        return bind(name, Blob.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final boolean value) {
        return bind(position, boolean.class, value);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Boolean value) {
        return bind(position, Boolean.class, value);
    }

    private This bind(final int position, final Class<?> type, final Object value) {
        return bindByType(position, value, type);
    }

    private This bind(final String name, final Class<?> type, final Object value) {
        return bindByType(name, value, type);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final boolean value) {
        return bindByType(name, value, boolean.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Boolean value) {
        return bindByType(name, value, Boolean.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final byte value) {
        return bindByType(position, value, byte.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Byte value) {
        return bindByType(position, value, Byte.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final byte value) {
        return bindByType(name, value, byte.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Byte value) {
        return bindByType(name, value, Byte.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final byte[] value) {
        return bindByType(position, value, byte[].class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final byte[] value) {
        return bindByType(name, value, byte[].class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     * @param length   number of characters to read
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Reader value, final int length) {

        return bind(position, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument by name
     *
     * @param name   token name to bind the parameter to
     * @param value  to bind
     * @param length number of characters to read
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Reader value, final int length) {
        return bind(name, new CharacterStreamArgument(value, length));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Clob value) {
        return bindByType(position, value, Clob.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Clob value) {
        return bindByType(name, value, Clob.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final java.sql.Date value) {
        return bindByType(position, value, java.sql.Date.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final java.sql.Date value) {
        return bindByType(name, value, java.sql.Date.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final java.util.Date value) {
        return bindByType(position, value, java.util.Date.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final java.util.Date value) {
        return bindByType(name, value, java.util.Date.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final double value) {
        return bindByType(position, value, double.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Double value) {
        return bindByType(position, value, Double.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final double value) {
        return bindByType(name, value, double.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Double value) {
        return bindByType(name, value, Double.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final float value) {
        return bindByType(position, value, float.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Float value) {
        return bindByType(position, value, Float.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final float value) {
        return bindByType(name, value, float.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Float value) {
        return bindByType(name, value, Float.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final long value) {
        return bindByType(position, value, long.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Long value) {
        return bindByType(position, value, Long.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final long value) {
        return bindByType(name, value, long.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Long value) {
        return bindByType(name, value, Long.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Short value) {
        return bindByType(position, value, Short.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final short value) {
        return bindByType(position, value, short.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final short value) {
        return bindByType(name, value, short.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Short value) {
        return bindByType(name, value, Short.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Object value) {
        getBinding().addPositional(position, value);
        return typedThis();
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Object value) {
        getBinding().addNamed(name, value);
        return typedThis();
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Time value) {
        return bindByType(position, value, Time.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Time value) {
        return bindByType(name, value, Time.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final Timestamp value) {
        return bindByType(position, value, Timestamp.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final Timestamp value) {
        return bindByType(name, value, Timestamp.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final URL value) {
        return bindByType(position, value, URL.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final URL value) {
        return bindByType(name, value, URL.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final URI value) {
        return bindByType(position, value, URI.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final URI value) {
        return bindByType(name, value, URI.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    default This bind(final int position, final UUID value) {
        return bindByType(position, value, UUID.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    default This bind(final String name, final UUID value) {
        return bindByType(name, value, UUID.class);
    }

    /**
     * Bind an argument dynamically by the type passed in.
     *
     * @param position     position to bind the parameter at, starting at 0
     * @param value        to bind
     * @param argumentType type for value argument
     *
     * @return the same Query instance
     */
    default This bindByType(final int position, final Object value, final Type argumentType) {
        return bindByType(position, value, QualifiedType.of(argumentType));
    }

    /**
     * Bind an argument dynamically by the generic type passed in.
     *
     * @param position     position to bind the parameter at, starting at 0
     * @param value        to bind
     * @param argumentType type token for value argument
     *
     * @return the same Query instance
     */
    default This bindByType(final int position, final Object value, final GenericType<?> argumentType) {
        return bindByType(position, value, argumentType.getType());
    }

    /**
     * Bind an argument dynamically by the qualified type passed in.
     *
     * @param position     position to bind the parameter at, starting at 0
     * @param value        to bind
     * @param argumentType type token for value argument
     *
     * @return the same Query instance
     */
    default This bindByType(final int position, final Object value, final QualifiedType<?> argumentType) {
        getBinding().addPositional(position, value, argumentType);
        return typedThis();
    }

    /**
     * Bind an argument dynamically by the type passed in.
     *
     * @param name         token name to bind the parameter to
     * @param value        to bind
     * @param argumentType type for value argument
     *
     * @return the same Query instance
     */
    default This bindByType(final String name, final Object value, final Type argumentType) {
        return bindByType(name, value, QualifiedType.of(argumentType));
    }

    /**
     * Bind an argument dynamically by the generic type passed in.
     *
     * @param name         token name to bind the parameter to
     * @param value        to bind
     * @param argumentType type token for value argument
     *
     * @return the same Query instance
     */
    default This bindByType(final String name, final Object value, final GenericType<?> argumentType) {
        return bindByType(name, value, argumentType.getType());
    }

    /**
     * Bind an argument dynamically by the type passed in.
     *
     * @param name         token name to bind the parameter to
     * @param value        to bind
     * @param argumentType type for value argument
     *
     * @return the same Query instance
     */
    default This bindByType(final String name, final Object value, final QualifiedType<?> argumentType) {
        getBinding().addNamed(name, value, argumentType);
        return typedThis();
    }

    /**
     * Bind a Java array as a SQL array.  Usually you can just {@link #bind(int, Object)} an array,
     * but this method allows varargs.
     * @param <T> the array element type
     * @param name the name of the parameter to bind
     * @param array the array to bind
     * @return this Query
     */
    default <T> This bindArray(final String name, @SuppressWarnings("unchecked") final T... array) {
        return bindArray(name, array.getClass().getComponentType(), array);
    }

    /**
     * Bind a Java array as a SQL array.  Usually you can just {@link #bind(int, Object)} an array,
     * but this method allows varargs.
     * @param <T> the array element type
     * @param pos the position of the parameter to bind
     * @param array the array to bind
     * @return this Query
     */
    default <T> This bindArray(final int pos, @SuppressWarnings("unchecked") final T... array) {
        return bindArray(pos, array.getClass().getComponentType(), array);
    }

    /**
     * Bind a Java array as a SQL array, casting each element to a new type.
     * @param name the name of the parameter to bind
     * @param elementType the array element type
     * @param array the array to bind
     * @return this Query
     */
    default This bindArray(final String name, final Type elementType, final Object... array) {
        return bindByType(name, array, arrayType(elementType));
    }

    /**
     * Bind a Java array as a SQL array, casting each element to a new type.
     * @param pos the position of the parameter to bind
     * @param elementType the array element type
     * @param array the array to bind
     * @return this Query
     */
    default This bindArray(final int pos, final Type elementType, final Object... array) {
        return bindByType(pos, array, arrayType(elementType));
    }

    /**
     * Bind an Iterable as a SQL array.
     * @param name the name of the parameter to bind
     * @param elementType the element type of the Iterable
     * @param iterable the iterable to bind as an array
     * @return this Query
     */
    default This bindArray(final String name, final Type elementType, final Iterable<?> iterable) {
        return bindByType(name, iterable, parameterizeClass(Iterable.class, elementType));
    }

    /**
     * Bind an Iterable as a SQL array.
     * @param pos the position of the parameter to bind
     * @param elementType the element type of the Iterable
     * @param iterable the iterable to bind as an array
     * @return this Query
     */
    default This bindArray(final int pos, final Type elementType, final Iterable<?> iterable) {
        return bindByType(pos, iterable, parameterizeClass(Iterable.class, elementType));
    }

    /**
     * Bind an Iterator as a SQL array.
     * @param name the name of the parameter to bind
     * @param elementType the element type of the Iterable
     * @param iterator the iterator to bind as an array
     * @return this Query
     */
    default This bindArray(final String name, final Type elementType, final Iterator<?> iterator) {
        return bindByType(name, iterator, parameterizeClass(Iterator.class, elementType));
    }

    /**
     * Bind an Iterator as a SQL array.
     * @param pos the position of the parameter to bind
     * @param elementType the element type of the Iterator
     * @param iterator the Iterator to bind as an array
     * @return this Query
     */
    default This bindArray(final int pos, final Type elementType, final Iterator<?> iterator) {
        return bindByType(pos, iterator, parameterizeClass(Iterator.class, elementType));
    }

    /**
     * Bind NULL to be set for a given argument.
     *
     * @param name    Named parameter to bind to
     * @param sqlType The sqlType must be set and is a value from <code>java.sql.Types</code>
     *
     * @return the same statement instance
     */
    default This bindNull(final String name, final int sqlType) {
        return bind(name, new NullArgument(sqlType));
    }

    /**
     * Bind NULL to be set for a given argument.
     *
     * @param position position to bind NULL to, starting at 0
     * @param sqlType  The sqlType must be set and is a value from <code>java.sql.Types</code>
     *
     * @return the same statement instance
     */
    default This bindNull(final int position, final int sqlType) {
        return bind(position, new NullArgument(sqlType));
    }

    /**
     * Bind a value using a specific type from <code>java.sql.Types</code> via
     * PreparedStatement#setObject(int, Object, int)
     *
     * @param name    Named parameter to bind at
     * @param value   Value to bind
     * @param sqlType The sqlType from java.sql.Types
     *
     * @return self
     */
    default This bindBySqlType(final String name, final Object value, final int sqlType) {
        return bind(name, ObjectArgument.of(value, sqlType));
    }

    /**
     * Bind a value using a specific type from <code>java.sql.Types</code> via
     * PreparedStatement#setObject(int, Object, int)
     *
     * @param position position to bind NULL to, starting at 0
     * @param value    Value to bind
     * @param sqlType  The sqlType from java.sql.Types
     *
     * @return self
     */
    default This bindBySqlType(final int position, final Object value, final int sqlType) {
        return bind(position, ObjectArgument.of(value, sqlType));
    }

    /**
     * see {@link #bindList(BiConsumer, String, List)}
     *
     * @param key    attribute name
     * @param values vararg values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the vararg array is empty.
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindList(final String key, final Object... values) {
        return bindList(EmptyHandling.THROW, key, values);
    }

    /**
     * see {@link #bindList(BiConsumer, String, List)}
     *
     * @param onEmpty handler for null/empty vararg array
     * @param key     attribute name
     * @param values  vararg values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the vararg array is empty.
     * @see EmptyHandling
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindList(final BiConsumer<SqlStatement<?>, String> onEmpty, final String key, final Object... values) {
        return bindList(onEmpty, key, values == null ? null : Arrays.asList(values));
    }

    /**
     * see {@link #bindList(BiConsumer, String, List)}
     *
     * @param key    attribute name
     * @param values iterable values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the iterable is empty.
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindList(final String key, final Iterable<?> values) {
        return bindList(EmptyHandling.THROW, key, values);
    }

    /**
     * see {@link #bindList(BiConsumer, String, List)}
     *
     * @param onEmpty handler for null/empty list
     * @param key     attribute name
     * @param values  iterable values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the iterable is empty.
     * @see EmptyHandling
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindList(final BiConsumer<SqlStatement<?>, String> onEmpty, final String key, final Iterable<?> values) {
        return bindList(onEmpty, key, values == null ? null : IterableLike.toList(values));
    }

    /**
     * see {@link #bindList(BiConsumer, String, List)}
     *
     * @param key    attribute name
     * @param values iterator of values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the iterator is empty.
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindList(final String key, final Iterator<?> values) {
        return bindList(EmptyHandling.THROW, key, values);
    }

    /**
     * see {@link #bindList(BiConsumer, String, List)}
     *
     * @param onEmpty handler for null/empty list
     * @param key     attribute name
     * @param values  iterator of values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the iterator is empty.
     * @see EmptyHandling
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindList(final BiConsumer<This, String> onEmpty, final String key, final Iterator<?> values) {
        return bindList(onEmpty, key, values == null ? null : IterableLike.toList(values));
    }

    /**
     * Bind a parameter for each value in the given list, and defines an attribute as the comma-separated list of
     * parameter references (using colon prefix).
     * <p>
     * Examples:
     * <pre>
     * List&lt;String&gt; columnNames = Arrays.asList("id", "name", "created_on");
     * List&lt;Object&gt; values = Arrays.asList(1, "Alice", LocalDate.now());
     * handle.createUpdate("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
     *     .defineList("columnNames", columnNames)
     *     .bindList("values", values)
     *     .execute();
     *
     * List&lt;Integer&gt; ids = Arrays.asList(1, 2, 3);
     * List&lt;Thing&gt; things = handle.createQuery("select * from things where id in (&lt;ids&gt;)")
     *     .bindList("ids", ids)
     *     .mapTo(Contact.class)
     *     .list();
     * </pre>
     *
     * Note that using this method modifies the SQL statement by using a defined attribute. This is
     * problematic when using {@link Handle#prepareBatch(String)} or the {@link PreparedBatch} SQL
     * operation as those evaluate the SQL statement only once. When binding lists of different size,
     * the number of placeholders will not match the number of elements in the list which will lead
     * to errors.
     *
     * @param onEmpty handler for null/empty list
     * @param key     attribute name
     * @param values  list of values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the list is empty.
     * @see EmptyHandling
     */
    default This bindList(final BiConsumer<This, String> onEmpty, final String key, final List<?> values) {
        if (values == null || values.isEmpty()) {
            onEmpty.accept(typedThis(), key);
            return typedThis();
        }

        final StringBuilder names = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            final String name = "__" + key + "_" + i;

            if (i > 0) {
                names.append(',');
            }
            final String paramName = getConfig()
                    .get(SqlStatements.class)
                    .getSqlParser()
                    .nameParameter(name, /* XXX */ null);
            names.append(paramName);

            bind(name, values.get(i));
        }

        return define(key, names.toString());
    }

    /**
     * Bind a parameter for each value in the given list * number of property names,
     * and defines an attribute as the comma-separated list of parameter references (using colon prefix).
     *
     * Used to create query similar to:
     * select * from things where (id, foo) in ((1,'abc'),(2,'def'),(3,'ghi'))
     * <p>
     * Examples:
     * <pre>
     *
     * List&lt;ThingKey&gt; thingKeys = ...
     * List&lt;Thing&gt; things = handle.createQuery("select * from things where (id, foo) in (&lt;thingKeys&gt;)")
     *     .bindBeanList("thingKeys", thingKeys, Arrays.asList("id", "foo"))
     *     .mapTo(Contact.class)
     *     .list();
     * </pre>
     *
     * @param key    attribute name
     * @param values list of values that will be comma-spliced into the defined attribute value.
     * @param propertyNames list of properties that will be invoked on the values.
     * @return this
     * @throws IllegalArgumentException if the list of values or properties is empty.
     * @throws UnableToCreateStatementException If a property can't be found on an value or we can't find a Argument for it.
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindBeanList(final String key, final List<?> values, final List<String> propertyNames) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".bindBeanList was called with no values.");
        }

        if (propertyNames.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".bindBeanList was called with no properties.");
        }

        final StringBuilder names = new StringBuilder();

        for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            if (valueIndex > 0) {
                names.append(',');
            }

            final Object bean = values.get(valueIndex);
            final BeanPropertyArguments beanProperties = new BeanPropertyArguments(null, bean, getConfig());

            names.append('(');
            for (int propertyIndex = 0; propertyIndex < propertyNames.size(); propertyIndex++) {
                if (propertyIndex > 0) {
                    names.append(',');
                }
                final String propertyName = propertyNames.get(propertyIndex);
                final String name = "__" + key + '_' + valueIndex + '_' + propertyName;
                names.append(':').append(name);
                final Argument argument = beanProperties.find(propertyName, getConfig())
                        .orElseThrow(() -> new UnableToCreateStatementException("Unable to get " + propertyName + " argument for " + bean));
                bind(name, argument);
            }
            names.append(')');
        }

        return define(key, names.toString());
    }

    /**
     * For each value given, create a tuple by invoking each given method in order, and bind the tuple into
     * a {@code VALUES (...)} format insert clause.
     * @param key attribute name
     * @param values list of values that will be comma-spliced into the defined attribute value
     * @param methodNames list of methods that will be invoked on the values
     * @return this
     * @throws IllegalArgumentException if the list of values or properties is empty.
     * @throws UnableToCreateStatementException if the method cannot be found
     * @see #bindList(BiConsumer, String, List)
     */
    default This bindMethodsList(final String key, final Iterable<?> values, final List<String> methodNames) {
        final Iterator<?> valueIter = values.iterator();
        if (!valueIter.hasNext()) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + ".bindMethodsList was called with no values.");
        }

        if (methodNames.isEmpty()) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + ".bindMethodsList was called with no values.");
        }

        final StringBuilder names = new StringBuilder();
        for (int valueIndex = 0; valueIter.hasNext(); valueIndex++) {
            if (valueIndex > 0) {
                names.append(',');
            }

            final Object bean = valueIter.next();
            final ObjectMethodArguments beanMethods = new ObjectMethodArguments(null, bean);

            names.append('(');
            for (int methodIndex = 0; methodIndex < methodNames.size(); methodIndex++) {
                if (methodIndex > 0) {
                    names.append(',');
                }

                final String methodName = methodNames.get(methodIndex);
                final String name = key + valueIndex + '.' + methodName;
                names.append(':').append(name);
                final Argument argument = beanMethods.find(methodName, getConfig())
                    .orElseThrow(() -> new UnableToCreateStatementException("Unable to get " + methodName + " argument for " + bean));
                bind(name, argument);
            }
            names.append(')');
        }

        return define(key, names.toString());
    }

    /**
     * Define an attribute as the comma-separated {@link String} from the elements of the {@code values} argument.
     * <p>
     * Examples:
     * <pre>
     * handle.createUpdate("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
     *     .defineList("columnNames", "id", "name", "created_on")
     *     .bindList("values", 1, "Alice", LocalDate.now())
     *     .execute();
     *
     * List&lt;Thing&gt; things = handle.createQuery("select &lt;columnNames&gt; from things")
     *     .bindList("columnNames", "id", "name", "created_on")
     *     .mapTo(Contact.class)
     *     .list();
     * </pre>
     *
     * @param key    attribute name
     * @param values vararg values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the vararg array is empty, or contains any null elements.
     */
    default This defineList(final String key, final Object... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".defineList was called with no vararg values.");
        }
        if (Stream.of(values).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".defineList was called with a vararg array containing null values.");
        }

        return defineList(key, Arrays.asList(values));
    }

    /**
     * Define an attribute as the comma-separated {@link String} from the elements of the {@code values} argument.
     * <p>
     * Examples:
     * <pre>
     * List&lt;String&gt; columnNames = Arrays.asList("id", "name", "created_on");
     * List&lt;Object&gt; values = Arrays.asList(1, "Alice", LocalDate.now());
     * handle.createUpdate("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
     *     .defineList("columnNames", columnNames)
     *     .bindList("values", 1, values)
     *     .execute();
     *
     * List&lt;String&gt; columnNames = Arrays.asList("id", "name", "created_on");
     * List&lt;Thing&gt; things = handle.createQuery("select &lt;columnNames&gt; from things")
     *     .bindList("columnNames", columnNames)
     *     .mapTo(Contact.class)
     *     .list();
     * </pre>
     *
     * @param key    attribute name
     * @param values list of values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the list is empty, or contains any null elements.
     */
    default This defineList(final String key, final List<?> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".defineList was called with an empty list.");
        }
        // Uses stream match, cause the Java 9 ImmutableList implementation throws an NPE if asked `contains(null)`
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".defineList was called with a list containing null values.");
        }

        final String value = values.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));

        return define(key, value);
    }
}

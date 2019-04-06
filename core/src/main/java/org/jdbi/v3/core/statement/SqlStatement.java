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
package org.jdbi.v3.core.statement;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.BeanPropertyArguments;
import org.jdbi.v3.core.argument.CharacterStreamArgument;
import org.jdbi.v3.core.argument.InputStreamArgument;
import org.jdbi.v3.core.argument.MapArguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.NullArgument;
import org.jdbi.v3.core.argument.ObjectArgument;
import org.jdbi.v3.core.argument.ObjectFieldArguments;
import org.jdbi.v3.core.argument.ObjectMethodArguments;
import org.jdbi.v3.core.argument.internal.PojoPropertyArguments;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.Mappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.jdbi.v3.core.qualifier.NVarchar;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Beta;

import static java.util.stream.Collectors.joining;

/**
 * This class provides the common functions between <code>Query</code> and
 * <code>Update</code>. It defines most of the argument binding functions
 * used by its subclasses.
 */
@SuppressWarnings({"deprecation", "PMD.ExcessiveClassLength"})
public abstract class SqlStatement<This extends SqlStatement<This>> extends BaseStatement<This> {
    private final Handle handle;
    private final String sql;

    /**
     * This will be set on execution, not before
     */
    private PreparedStatement stmt;

    SqlStatement(Handle handle,
                 String sql) {
        super(handle);

        this.handle = handle;
        this.sql = sql;

        getContext()
            .setConnection(handle.getConnection())
            .setRawSql(sql);
    }

    protected Binding getBinding() {
        return getContext().getBinding();
    }

    /**
     * @return the un-translated SQL used to create this statement
     */
    protected String getSql() {
        return sql;
    }

    /**
     * Set the query timeout, in seconds, on the prepared statement
     *
     * @param seconds number of seconds before timing out
     *
     * @return the same Query instance
     */
    public This setQueryTimeout(final int seconds) {
        return addCustomizer(StatementCustomizers.statementTimeout(seconds));
    }

    /**
     * Transfer ownership of the handle to the statement: when the statement is closed,
     * commit the handle's transaction (if one exists) and close the handle.
     * @return this
     */
    public This cleanupHandleCommit() {
        return cleanupHandle(Handle::commit);
    }

    /**
     * When the statement is closed, roll it back then close the owning Handle.
     * @return this
     */
    public This cleanupHandleRollback() {
        return cleanupHandle(Handle::rollback);
    }

    private This cleanupHandle(Consumer<Handle> action) {
        addCleanable(() -> {
            if (handle != null) {
                if (handle.isInTransaction()) {
                    action.accept(handle);
                }
                handle.close();
            }
        });
        return typedThis;
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param position position to bindBinaryStream this argument, starting at 0
     * @param argument exotic argument factory
     *
     * @return the same Query instance
     */
    @SuppressWarnings("unchecked")
    public This bind(int position, Argument argument) {
        getBinding().addPositional(position, argument);
        return (This) this;
    }

    /**
     * Used if you need to have some exotic parameter bound.
     *
     * @param name     name to bindBinaryStream this argument
     * @param argument exotic argument factory
     *
     * @return the same Query instance
     */
    public This bind(String name, Argument argument) {
        getBinding().addNamed(name, argument);
        return typedThis;
    }

    /**
     * Binds named parameters from JavaBean properties on the argument.
     *
     * @param bean source of named parameter values to use as arguments
     *
     * @return modified statement
     */
    @SuppressWarnings("deprecation")
    public This bindBean(Object bean) {
        return bindNamedArgumentFinder(new BeanPropertyArguments(null, bean));
    }

    /**
     * Binds named parameters from JavaBean properties on the bean argument, with the given prefix.
     *
     * Example: the prefix {@code foo} applied to a bean property {@code bar} will be bound as {@code foo.bar}.
     *
     * @param prefix a prefix to apply to all property names.
     * @param bean source of named parameter values to use as arguments
     *
     * @return modified statement
     */
    @SuppressWarnings("deprecation")
    public This bindBean(String prefix, Object bean) {
        return bindNamedArgumentFinder(new BeanPropertyArguments(prefix, bean));
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
    public This bindPojo(Object pojo) {
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
    public This bindPojo(String prefix, Object pojo) {
        return bindNamedArgumentFinder(new PojoPropertyArguments(prefix, pojo, getConfig()));
    }

    /**
     * Binds public fields of the specified object as arguments for the query.
     *
     * @param object source of the public fields to bind.
     *
     * @return modified statement
     */
    public This bindFields(Object object) {
        return bindNamedArgumentFinder(new ObjectFieldArguments(null, object));
    }

    /**
     * Binds public fields of the specified object as arguments for the query.
     *
     * @param prefix a prefix to apply to all field names.
     * @param object source of the public fields to bind.
     *
     * @return modified statement
     */
    public This bindFields(String prefix, Object object) {
        return bindNamedArgumentFinder(new ObjectFieldArguments(prefix, object));
    }

    /**
     * Binds methods with no parameters on the argument.
     *
     * @param object source of methods to use as arguments
     *
     * @return modified statement
     */
    public This bindMethods(Object object) {
        return bindNamedArgumentFinder(new ObjectMethodArguments(null, object));
    }

    /**
     * Binds methods with no parameters on the argument, with the given prefix.
     *
     * @param prefix a prefix to apply to all property names.
     * @param object source of methods to use as arguments
     *
     * @return modified statement
     */
    public This bindMethods(String prefix, Object object) {
        return bindNamedArgumentFinder(new ObjectMethodArguments(prefix, object));
    }

    /**
     * Binds named parameters from a map of String to Object instances
     *
     * @param map map where keys are matched to named parameters in order to bind arguments.
     *            Can be null, in which case the binding has no effect.
     *
     * @return modified statement
     */
    public This bindMap(Map<String, ?> map) {
        return map == null ? typedThis : bindNamedArgumentFinder(new MapArguments(map));
    }

    /**
     * Binds a new {@link NamedArgumentFinder}.
     *
     * @param namedArgumentFinder A NamedArgumentFinder to bind. Can be null.
     *
     * @return the same Query instance
     */
    public This bindNamedArgumentFinder(final NamedArgumentFinder namedArgumentFinder) {
        if (namedArgumentFinder != null) {
            getBinding().addNamedArgumentFinder(namedArgumentFinder);
        }

        return typedThis;
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Character value) {
        return bind(position, toArgument(Character.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Character value) {
        return bind(name, toArgument(Character.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, String value) {
        return bind(position, toArgument(String.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, String value) {
        return bind(name, toArgument(String.class, value));
    }

    /**
     * Bind a {@code String} argument positionally, as {@code NVARCHAR} type.
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value to bind
     *
     * @return the same Query instance
     */
    @Beta
    public final This bindNVarchar(int position, String value) {
        return bind(position, toArgument(QualifiedType.of(String.class).with(NVarchar.class), value));
    }

    /**
     * Bind a {@code String} argument by name, as {@code NVARCHAR} type.
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    @Beta
    public final This bindNVarchar(String name, String value) {
        return bind(name, toArgument(QualifiedType.of(String.class).with(NVarchar.class), value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, int value) {
        return bind(position, toArgument(int.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Integer value) {
        return bind(position, toArgument(Integer.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, int value) {
        return bind(name, toArgument(int.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Integer value) {
        return bind(name, toArgument(Integer.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, char value) {
        return bind(position, toArgument(char.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, char value) {
        return bind(name, toArgument(char.class, value));
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
    public final This bindASCIIStream(int position, InputStream value, int length) {
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
    public final This bindASCIIStream(String name, InputStream value, int length) {
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
    public final This bind(int position, BigDecimal value) {
        return bind(position, toArgument(BigDecimal.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, BigDecimal value) {
        return bind(name, toArgument(BigDecimal.class, value));
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
    public final This bindBinaryStream(int position, InputStream value, int length) {
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
    public final This bindBinaryStream(String name, InputStream value, int length) {
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
    public final This bind(int position, Blob value) {
        return bind(position, toArgument(Blob.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Blob value) {
        return bind(name, toArgument(Blob.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, boolean value) {
        return bind(position, toArgument(boolean.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Boolean value) {
        return bind(position, toArgument(Boolean.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, boolean value) {
        return bind(name, toArgument(boolean.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Boolean value) {
        return bind(name, toArgument(Boolean.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, byte value) {
        return bind(position, toArgument(byte.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Byte value) {
        return bind(position, toArgument(Byte.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, byte value) {
        return bind(name, toArgument(byte.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Byte value) {
        return bind(name, toArgument(Byte.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, byte[] value) {
        return bind(position, toArgument(byte[].class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, byte[] value) {
        return bind(name, toArgument(byte[].class, value));
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
    public final This bind(int position, Reader value, int length) {

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
    public final This bind(String name, Reader value, int length) {
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
    public final This bind(int position, Clob value) {
        return bind(position, toArgument(Clob.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Clob value) {
        return bind(name, toArgument(Clob.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, java.sql.Date value) {
        return bind(position, toArgument(java.sql.Date.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, java.sql.Date value) {
        return bind(name, toArgument(java.sql.Date.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, java.util.Date value) {
        return bind(position, toArgument(java.util.Date.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, java.util.Date value) {
        return bind(name, toArgument(java.util.Date.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, double value) {
        return bind(position, toArgument(double.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Double value) {
        return bind(position, toArgument(Double.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, double value) {
        return bind(name, toArgument(double.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Double value) {
        return bind(name, toArgument(Double.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, float value) {
        return bind(position, toArgument(float.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Float value) {
        return bind(position, toArgument(Float.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, float value) {
        return bind(name, toArgument(float.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Float value) {
        return bind(name, toArgument(Float.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, long value) {
        return bind(position, toArgument(long.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Long value) {
        return bind(position, toArgument(Long.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, long value) {
        return bind(name, toArgument(long.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Long value) {
        return bind(name, toArgument(Long.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Short value) {
        return bind(position, toArgument(Short.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, short value) {
        return bind(position, toArgument(short.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, short value) {
        return bind(name, toArgument(short.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Short value) {
        return bind(name, toArgument(short.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Object value) {
        return bind(position, toArgument(value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Object value) {
        return bind(name, toArgument(value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Time value) {
        return bind(position, toArgument(Time.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Time value) {
        return bind(name, toArgument(Time.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Timestamp value) {
        return bind(position, toArgument(Timestamp.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Timestamp value) {
        return bind(name, toArgument(Timestamp.class, value));
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, URL value) {
        return bind(position, toArgument(URL.class, value));
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, URL value) {
        return bind(name, toArgument(URL.class, value));
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
    public final This bindByType(int position, Object value, Type argumentType) {
        return bind(position, toArgument(argumentType, value));
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
    public final This bindByType(int position, Object value, GenericType<?> argumentType) {
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
    @Beta
    public final This bindByType(int position, Object value, QualifiedType<?> argumentType) {
        return bind(position, toArgument(argumentType, value));
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
    public final This bindByType(String name, Object value, Type argumentType) {
        return bind(name, toArgument(argumentType, value));
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
    public final This bindByType(String name, Object value, GenericType<?> argumentType) {
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
    @Beta
    public final This bindByType(String name, Object value, QualifiedType<?> argumentType) {
        return bind(name, toArgument(argumentType, value));
    }

    private Argument toArgument(Object value) {
        return toArgument(value == null ? Object.class : value.getClass(), value);
    }

    private Argument toArgument(Type type, Object value) {
        Argument arg = getConfig(Arguments.class).findFor(type, value)
            .orElseThrow(() -> factoryNotFound(type, value));

        try {
            boolean toStringIsImplementedInArgument = arg.getClass().getMethod("toString").getDeclaringClass() != Object.class;

            if (toStringIsImplementedInArgument) {
                return arg;
            } else {
                return new Argument() {
                    @Override
                    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
                        arg.apply(position, statement, ctx);
                    }

                    @Override
                    public String toString() {
                        return Objects.toString(value);
                    }
                };
            }
        } catch (NoSuchMethodException e) {
            throw new Error("toString method does not exist, Object hierarchy is corrupt", e);
        }
    }

    private Argument toArgument(QualifiedType<?> type, Object value) {
        return getConfig(Arguments.class).findFor(type, value)
                .orElseThrow(() -> factoryNotFound(type, value));
    }

    private UnsupportedOperationException factoryNotFound(Type type, Object value) {
        if (type instanceof Class<?>) { // not a ParameterizedType
            final TypeVariable<?>[] params = ((Class<?>) type).getTypeParameters();
            if (params.length > 0) {
                return new UnsupportedOperationException("No type parameters found for erased type '" + type + Arrays.toString(params)
                    + "'. To bind a generic type, prefer using bindByType.");
            }
        }
        return new UnsupportedOperationException("No argument factory registered for '" + value + "' of type " + type);
    }

    private UnsupportedOperationException factoryNotFound(QualifiedType<?> qualifiedType, Object value) {
        Type type = qualifiedType.getType();
        if (type instanceof Class<?>) { // not a ParameterizedType
            final TypeVariable<?>[] params = ((Class<?>) type).getTypeParameters();
            if (params.length > 0) {
                return new UnsupportedOperationException("No type parameters found for erased type '" + type + Arrays.toString(params)
                    + "' with qualifiers '" + qualifiedType.getQualifiers()
                    + "'. To bind a generic type, prefer using bindByType.");
            }
        }
        return new UnsupportedOperationException("No argument factory registered for '" + value + "' of qualified type " + qualifiedType);
    }

    /**
     * Bind NULL to be set for a given argument.
     *
     * @param name    Named parameter to bind to
     * @param sqlType The sqlType must be set and is a value from <code>java.sql.Types</code>
     *
     * @return the same statement instance
     */
    public final This bindNull(String name, int sqlType) {
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
    public final This bindNull(int position, int sqlType) {
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
    public final This bindBySqlType(String name, Object value, int sqlType) {
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
    public final This bindBySqlType(int position, Object value, int sqlType) {
        return bind(position, ObjectArgument.of(value, sqlType));
    }

    /**
     * Bind a parameter for each value in the given vararg array, and defines an attribute as the comma-separated list
     * of parameter references (using colon prefix).
     * <p>
     * Examples:
     * <pre>
     * handle.createUpdate("insert into things (&lt;columnNames&gt;) values (&lt;values&gt;)")
     *     .defineList("columnNames", "id", "name", "created_on")
     *     .bindList("values", 1, "Alice", LocalDate.now())
     *     .execute();
     *
     * List&lt;Thing&gt; things = handle.createQuery("select * from things where id in (&lt;ids&gt;)")
     *     .bindList("ids", 1, 2, 3)
     *     .mapTo(Contact.class)
     *     .list();
     * </pre>
     *
     * @param key    attribute name
     * @param values vararg values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the vararg array is empty.
     */
    public final This bindList(String key, Object... values) {
        if (values.length == 0) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".bindList was called with no vararg values.");
        }

        return bindList(key, Arrays.asList(values));
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
     * @param key    attribute name
     * @param values list of values that will be comma-spliced into the defined attribute value.
     * @return this
     * @throws IllegalArgumentException if the list is empty.
     */
    public final This bindList(String key, List<?> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".bindList was called with an empty list.");
        }

        StringBuilder names = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            String name = "__" + key + "_" + i;

            if (i > 0) {
                names.append(',');
            }
            String paramName = getConfig().get(SqlStatements.class).getSqlParser().nameParameter(name, getContext());
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
     */
    public final This bindBeanList(String key, List<?> values, List<String> propertyNames) throws UnableToCreateStatementException {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".bindBeanList was called with no values.");
        }

        if (propertyNames.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".bindBeanList was called with no properties.");
        }

        StringBuilder names = new StringBuilder();

        StatementContext ctx = getContext();
        for (int valueIndex = 0; valueIndex < values.size(); valueIndex++) {
            if (valueIndex > 0) {
                names.append(',');
            }

            Object bean = values.get(valueIndex);
            BeanPropertyArguments beanProperties = new BeanPropertyArguments(null, bean);

            names.append('(');
            for (int propertyIndex = 0; propertyIndex < propertyNames.size(); propertyIndex++) {
                if (propertyIndex > 0) {
                    names.append(',');
                }
                String propertyName = propertyNames.get(propertyIndex);
                String name = "__" + key + '_' + valueIndex + '_' + propertyName;
                names.append(':').append(name);
                Argument argument = beanProperties.find(propertyName, ctx)
                        .orElseThrow(() -> new UnableToCreateStatementException("Unable to get " + propertyName + " argument for " + bean, ctx));
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
     */
    public final This bindMethodsList(String key, Iterable<?> values, List<String> methodNames) throws UnableToCreateStatementException {
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
        final StatementContext ctx = getContext();
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
                final Argument argument = beanMethods.find(methodName, ctx)
                    .orElseThrow(() -> new UnableToCreateStatementException("Unable to get " + methodName + " argument for " + bean, ctx));
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
    public final This defineList(String key, Object... values) {
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
    public final This defineList(String key, List<?> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".defineList was called with an empty list.");
        }
        // Uses stream match, cause the Java 9 ImmutableList implementation throws an NPE if asked `contains(null)`
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    getClass().getSimpleName() + ".defineList was called with a list containing null values.");
        }

        String value = values.stream()
                .map(Object::toString)
                .collect(joining(", "));

        return define(key, value);
    }

    /**
     * Define all bound arguments that don't already have a definition with a boolean indicating their presence.
     * Useful to easily template optional properties of pojos or beans like {@code <if(property)>property = :property<endif>}.
     * @return this
     */
    @Beta
    public This defineNamedBindings() {
        return addCustomizer(new DefineNamedBindingsStatementCustomizer());
    }

    PreparedStatement internalExecute() {
        final StatementContext ctx = getContext();

        beforeTemplating();

        String renderedSql = getConfig(SqlStatements.class)
                .getTemplateEngine()
                .render(sql, ctx);
        ctx.setRenderedSql(renderedSql);

        ParsedSql parsedSql = getConfig(SqlStatements.class)
                .getSqlParser()
                .parse(renderedSql, ctx);
        String sql = parsedSql.getSql();
        ctx.setParsedSql(parsedSql);

        try {
            if (getClass().isAssignableFrom(Call.class)) {
                stmt = handle.getStatementBuilder().createCall(handle.getConnection(), sql, ctx);
            } else {
                stmt = handle.getStatementBuilder().create(handle.getConnection(), sql, ctx);
            }

            // The statement builder might (or might not) clean up the statement when called. E.g. the
            // caching statement builder relies on the statement *not* being closed.
            addCleanable(() -> handle.getStatementBuilder().close(handle.getConnection(), this.sql, stmt));
            getConfig(SqlStatements.class).customize(stmt);
        } catch (SQLException e) {
            throw new UnableToCreateStatementException(e, ctx);
        }

        ctx.setStatement(stmt);

        beforeBinding();

        ArgumentBinder.bind(parsedSql.getParameters(), getBinding(), stmt, ctx);

        beforeExecution();

        try {
            SqlLoggerUtil.wrap(stmt::execute, ctx, getConfig(SqlStatements.class).getSqlLogger());
        } catch (SQLException e) {
            try {
                stmt.close();
            } catch (SQLException e1) {
                e.addSuppressed(e1);
            }

            throw new UnableToExecuteStatementException(e, ctx);
        }

        afterExecution();

        return stmt;
    }

    @SuppressWarnings("unchecked")
    <T> RowMapper<T> mapperForType(Class<T> type) {
        return (RowMapper<T>) mapperForType((Type) type);
    }

    @SuppressWarnings("unchecked")
    <T> RowMapper<T> mapperForType(GenericType<T> type) {
        return (RowMapper<T>) mapperForType(type.getType());
    }

    RowMapper<?> mapperForType(Type type) {
        return getConfig(Mappers.class).findFor(type)
            .orElseThrow(() -> new UnsupportedOperationException("No mapper registered for " + type));
    }

    void beforeTemplating() {
        callCustomizers(c -> c.beforeTemplating(stmt, getContext()));
    }

    void beforeBinding() {
        callCustomizers(c -> c.beforeBinding(stmt, getContext()));
    }

    void beforeExecution() {
        callCustomizers(c -> c.beforeExecution(stmt, getContext()));
    }

    void afterExecution() {
        callCustomizers(c -> c.afterExecution(stmt, getContext()));
    }
}

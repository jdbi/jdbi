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
package org.jdbi.v3.core;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

import org.jdbi.v3.core.argument.AsciiStream;
import org.jdbi.v3.core.argument.BinaryStream;
import org.jdbi.v3.core.argument.CharacterStream;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.argument.NullValue;
import org.jdbi.v3.core.argument.ObjectArgument;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.rewriter.RewrittenStatement;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.core.statement.StatementCustomizers;
import org.jdbi.v3.core.transaction.TransactionState;
import org.jdbi.v3.core.util.GenericType;
import org.jdbi.v3.core.util.GenericTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides the common functions between <code>Query</code> and
 * <code>Update</code>. It defines most of the argument binding functions
 * used by its subclasses.
 */
public abstract class SqlStatement<This extends SqlStatement<This>> extends BaseStatement<This> {
    private static final Logger LOG = LoggerFactory.getLogger(SqlStatement.class);

    private final This             typedThis;
    private final Binding          params;
    private final Handle           handle;
    private final String           sql;
    private final StatementBuilder statementBuilder;

    /**
     * This will be set on execution, not before
     */
    private       RewrittenStatement rewritten;
    private       PreparedStatement  stmt;

    @SuppressWarnings("unchecked")
    SqlStatement(ConfigRegistry config,
                 Binding params,
                 Handle handle,
                 StatementBuilder statementBuilder,
                 String sql,
                 StatementContext ctx,
                 Collection<StatementCustomizer> statementCustomizers) {
        super(config, ctx);
        assert verifyOurNastyDowncastIsOkay();

        addCustomizers(statementCustomizers);

        this.typedThis = (This) this;
        this.statementBuilder = statementBuilder;
        this.handle = handle;
        this.sql = sql;
        this.params = params;

        ctx.setConnection(handle.getConnection());
        ctx.setRawSql(sql);
        ctx.setBinding(params);
    }

    public This setFetchDirection(final int value)
    {
        addStatementCustomizer(new StatementCustomizers.FetchDirectionStatementCustomizer(value));
        return typedThis;
    }

    /**
     * Provides a means for custom statement modification. Common cusotmizations
     * have their own methods, such as {@link Query#setMaxRows(int)}
     *
     * @param customizer instance to be used to cstomize a statement
     *
     * @return modified statement
     */
    public This addStatementCustomizer(StatementCustomizer customizer)
    {
        super.addCustomizer(customizer);
        return typedThis;
    }

    private boolean verifyOurNastyDowncastIsOkay()
    {
        // Prevent bogus signatures like Update extends SqlStatement<Query>
        // SqlStatement's generic parameter must be supertype of getClass()
        return GenericTypes.findGenericParameter(getClass(), SqlStatement.class)
                .map(GenericTypes::getErasedType)
                .map(type -> type.isAssignableFrom(getClass()))
                .orElse(true); // subclass is raw type.. ¯\_(ツ)_/¯
    }

    protected StatementBuilder getStatementBuilder()
    {
        return statementBuilder;
    }

    protected Binding getParams()
    {
        return params;
    }

    protected Handle getHandle()
    {
        return handle;
    }

    /**
     * @return the un-translated SQL used to create this statement
     */
    protected String getSql()
    {
        return sql;
    }

    /**
     * Set the query timeout, in seconds, on the prepared statement
     *
     * @param seconds number of seconds before timing out
     *
     * @return the same Query instance
     */
    public This setQueryTimeout(final int seconds)
    {
        return addStatementCustomizer(new StatementCustomizers.QueryTimeoutCustomizer(seconds));
    }

    /**
     * Close the handle when the statement is closed.
     *
     * @return the same Query instance
     */
    public This cleanupHandle()
    {
        super.addCleanable(Cleanables.forHandle(handle, TransactionState.ROLLBACK));
        return typedThis;
    }

    /**
     * Force transaction state when the statement is cleaned up.
     *
     * @param state the transaction state to enforce.
     *
     * @return the same Query instance
     */
    public This cleanupHandle(final TransactionState state)
    {
        super.addCleanable(Cleanables.forHandle(handle, state));
        return typedThis;
    }

    /**
     * Binds named parameters from JavaBean properties on the argument.
     *
     * @param bean source of named parameter values to use as arguments
     *
     * @return modified statement
     */
    public This bindBean(Object bean)
    {
        return bindNamedArgumentFinder(new BeanPropertyArguments(null, bean, getContext()));
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
    public This bindBean(String prefix, Object bean)
    {
        return bindNamedArgumentFinder(new BeanPropertyArguments(prefix, bean, getContext()));
    }

    /**
     * Binds named parameters from a map of String to Object instances
     *
     * @param map map where keys are matched to named parameters in order to bind arguments.
     *            Can be null, in which case the binding has no effect.
     *
     * @return modified statement
     */
    public This bindMap(Map<String, ?> map)
    {
        return map == null ? typedThis : bindNamedArgumentFinder(new MapArguments(map));
    }

    /**
     * Binds a new {@link NamedArgumentFinder}.
     *
     * @param namedArgumentFinder A NamedArgumentFinder to bind. Can be null.
     *
     * @return the same Query instance
     */
    public This bindNamedArgumentFinder(final NamedArgumentFinder namedArgumentFinder)
    {
        if (namedArgumentFinder != null) {
            getParams().addNamedArgumentFinder(namedArgumentFinder);
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
    public final This bind(int position, Character value)
    {
        return bind(position, value, Character.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Character value)
    {
        return bind(name, value, Character.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, String value)
    {
        return bind(position, value, String.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, String value)
    {
        return bind(name, value, String.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, int value)
    {
        return bind(position, value, int.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Integer value)
    {
        return bind(position, value, Integer.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, int value)
    {
        return bind(name, value, int.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Integer value)
    {
        return bind(name, value, Integer.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, char value)
    {
        return bind(position, value, char.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, char value)
    {
        return bind(name, value, char.class);
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
    public final This bindASCIIStream(int position, InputStream value, int length)
    {
        return bind(position, new AsciiStream(value, length), AsciiStream.class);
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
    public final This bindASCIIStream(String name, InputStream value, int length)
    {
        return bind(name, new AsciiStream(value, length), AsciiStream.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, BigDecimal value)
    {
        return bind(position, value, BigDecimal.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, BigDecimal value)
    {
        return bind(name, value, BigDecimal.class);
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
    public final This bindBinaryStream(int position, InputStream value, int length)
    {
        return bind(position, new BinaryStream(value, length), BinaryStream.class);
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
    public final This bindBinaryStream(String name, InputStream value, int length)
    {
        return bind(name, new BinaryStream(value, length), BinaryStream.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Blob value)
    {
        return bind(position, value, Blob.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Blob value)
    {
        return bind(name, value, Blob.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, boolean value)
    {
        return bind(position, value, boolean.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Boolean value)
    {
        return bind(position, value, Boolean.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, boolean value)
    {
        return bind(name, value, boolean.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Boolean value)
    {
        return bind(name, value, Boolean.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, byte value)
    {
        return bind(position, value, byte.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Byte value)
    {
        return bind(position, value, Byte.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, byte value)
    {
        return bind(name, value, byte.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Byte value)
    {
        return bind(name, value, Byte.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, byte[] value)
    {
        return bind(position, value, byte[].class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, byte[] value)
    {
        return bind(name, value, byte[].class);
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
    public final This bind(int position, Reader value, int length)
    {

        return bind(position, new CharacterStream(value, length), CharacterStream.class);
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
    public final This bind(String name, Reader value, int length)
    {
        return bind(name, new CharacterStream(value, length), CharacterStream.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Clob value)
    {
        return bind(position, value, Clob.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Clob value)
    {
        return bind(name, value, Clob.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, java.sql.Date value)
    {
        return bind(position, value, java.sql.Date.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, java.sql.Date value)
    {
        return bind(name, value, java.sql.Date.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, java.util.Date value)
    {
        return bind(position, value, java.util.Date.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, java.util.Date value)
    {
        return bind(name, value, java.util.Date.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, double value)
    {
        return bind(position, value, double.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Double value)
    {
        return bind(position, value, Double.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, double value)
    {
        return bind(name, value, double.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Double value)
    {
        return bind(name, value, Double.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, float value)
    {
        return bind(position, value, float.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Float value)
    {
        return bind(position, value, Float.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, float value)
    {
        return bind(name, value, float.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Float value)
    {
        return bind(name, value, Float.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, long value)
    {
        return bind(position, value, long.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Long value)
    {
        return bind(position, value, Long.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, long value)
    {
        return bind(name, value, long.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Long value)
    {
        return bind(name, value, Long.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Short value)
    {
        return bind(position, value, Short.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, short value)
    {
        return bind(position, value, short.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, short value)
    {
        return bind(name, value, short.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Short value)
    {
        return bind(name, value, short.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Object value)
    {
        value = maskNull(value);
        return bind(position, value, value.getClass());
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Object value)
    {
        value = maskNull(value);
        return bind(name, value, value.getClass());
    }

    private Object maskNull(Object value) {
        return value == null ? new NullValue() : value;
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Time value)
    {
        return bind(position, value, Time.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Time value)
    {
        return bind(name, value, Time.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, Timestamp value)
    {
        return bind(position, value, Timestamp.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, Timestamp value)
    {
        return bind(name, value, Timestamp.class);
    }

    /**
     * Bind an argument positionally
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     *
     * @return the same Query instance
     */
    public final This bind(int position, URL value)
    {
        return bind(position, value, URL.class);
    }

    /**
     * Bind an argument by name
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     *
     * @return the same Query instance
     */
    public final This bind(String name, URL value)
    {
        return bind(name, value, URL.class);
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
    public final This bind(int position, Object value, GenericType<?> argumentType)
    {
        return bind(position, value, argumentType.getType());
    }

    /**
     * Bind an argument dynamically by the type passed in.
     *
     * @param position position to bind the parameter at, starting at 0
     * @param value    to bind
     * @param type     type for value argument
     * @return the same Query instance
     */
    public final This bind(int position, Object value, Type type)
    {
        if (type.equals(Object.class)) {
            if (value == null) {
                value = new NullValue();
                type = NullValue.class;
            }
            else {
                type = value.getClass();
            }
        }
        getParams().addPositional(position, value, type);
        return typedThis;
    }

    /**
     * Bind an argument dynamically by the type passed in.
     *
     * @param name  token name to bind the parameter to
     * @param value to bind
     * @param type  type for value argument
     * @return the same Query instance
     */
    public final This bind(String name, Object value, Type type)
    {
        if (type.equals(Object.class)) {
            if (value == null) {
                value = new NullValue();
                type = NullValue.class;
            }
            else {
                type = value.getClass();
            }
        }
        getParams().addNamed(name, value, type);
        return typedThis;
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
    public final This bind(String name, Object value, GenericType<?> argumentType)
    {
        return bind(name, value, argumentType.getType());
    }

    /**
     * Bind NULL to be set for a given argument.
     *
     * @param name    Named parameter to bind to
     * @param sqlType The sqlType must be set and is a value from <code>java.sql.Types</code>
     *
     * @return the same statement instance
     */
    public final This bindNull(String name, int sqlType)
    {
        return bind(name, new NullValue(sqlType));
    }

    /**
     * Bind NULL to be set for a given argument.
     *
     * @param position position to bind NULL to, starting at 0
     * @param sqlType  The sqlType must be set and is a value from <code>java.sql.Types</code>
     *
     * @return the same statement instance
     */
    public final This bindNull(int position, int sqlType)
    {
        return bind(position, new NullValue(sqlType));
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
    public final This bindBySqlType(String name, Object value, int sqlType)
    {
        return bind(name, new ObjectArgument(sqlType));
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
    public final This bindBySqlType(int position, Object value, int sqlType)
    {
        return bind(position, new ObjectArgument(sqlType));
    }

    PreparedStatement internalExecute()
    {
        rewritten = getConfig(SqlStatements.class)
                .getStatementRewriter()
                .rewrite(sql, getParams(), getContext());
        getContext().setRewrittenSql(rewritten.getSql());
        try {
            if (getClass().isAssignableFrom(Call.class)) {
                stmt = statementBuilder.createCall(handle.getConnection(), rewritten.getSql(), getContext());
            }
            else {
                stmt = statementBuilder.create(handle.getConnection(), rewritten.getSql(), getContext());
            }
        }
        catch (SQLException e) {
            throw new UnableToCreateStatementException(e, getContext());
        }

        // The statement builder might (or might not) clean up the statement when called. E.g. the
        // caching statement builder relies on the statement *not* being closed.
        addCleanable(Cleanables.forStatementBuilder(statementBuilder, handle.getConnection(), sql, stmt));

        getContext().setStatement(stmt);

        try {
            rewritten.bind(getParams(), stmt);
        }
        catch (SQLException e) {
            throw new UnableToExecuteStatementException("Unable to bind parameters to query", e, getContext());
        }

        beforeExecution(stmt);

        try {
            final long start = System.nanoTime();
            stmt.execute();
            final long elapsedTime = System.nanoTime() - start;
            LOG.trace("Execute SQL \"{}\" in {}ms", rewritten.getSql(), elapsedTime / 1000000L);
            getConfig(SqlStatements.class)
                    .getTimingCollector()
                    .collect(elapsedTime, getContext());
        }
        catch (SQLException e) {
            try {
                stmt.close();
            } catch (SQLException e1) {
                e.addSuppressed(e1);
            }
            throw new UnableToExecuteStatementException(e, getContext());
        }

        afterExecution(stmt);

        return stmt;
    }

    @SuppressWarnings("unchecked")
    <T> RowMapper<T> rowMapperForType(Class<T> type)
    {
        return (RowMapper<T>) rowMapperForType((Type) type);
    }

    @SuppressWarnings("unchecked")
    <T> RowMapper<T> rowMapperForType(GenericType<T> type)
    {
        return (RowMapper<T>) rowMapperForType(type.getType());
    }

    RowMapper<?> rowMapperForType(Type type)
    {
        return getConfig().findRowMapperFor(type)
            .orElseThrow(() -> new UnsupportedOperationException("No mapper registered for " + type));
    }
}

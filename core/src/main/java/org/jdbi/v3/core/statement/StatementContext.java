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

import java.io.Closeable;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;

import jakarta.annotation.Nullable;

import org.jdbi.v3.core.CloseException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.array.SqlArrayArgumentStrategy;
import org.jdbi.v3.core.array.SqlArrayType;
import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.exceptions.ThrowableSuppressor;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.Mappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;

import static java.util.Objects.requireNonNull;

/**
 * The statement context provides access to statement-local configuration.
 * Context is inherited from the parent {@link Handle} initially and changes will
 * not outlive the statement.
 * The context will be passed through most major jdbi APIs.
 * <p>
 * DISCLAIMER: The class is not intended to be extended. The final modifier is absent to allow
 * mock tools to create a mock object of this class in the user code.
 */
public class StatementContext implements Closeable {

    private final ConfigRegistry config;
    private final ExtensionMethod extensionMethod;
    private final Type jdbiStatementType;

    private final Set<Cleanable> cleanables = new LinkedHashSet<>();

    private String rawSql;
    private String renderedSql;
    private ParsedSql parsedSql;
    private PreparedStatement statement;
    private Connection connection;
    private Binding binding = new Binding(this);

    private volatile boolean returningGeneratedKeys = false;
    private String[] generatedKeysColumnNames = new String[0];
    private volatile boolean concurrentUpdatable = false;

    private Instant executionMoment;
    private Instant completionMoment;
    private Instant exceptionMoment;
    private volatile long mappedRows;
    private String traceId;

    static StatementContext create(final ConfigRegistry config, final ExtensionMethod extensionMethod, final Type jdbiStatementType) {
        final StatementContext context = new StatementContext(config, extensionMethod, jdbiStatementType);
        context.notifyContextCreated();
        return context;
    }

    private StatementContext(final ConfigRegistry config, final ExtensionMethod extensionMethod, final Type jdbiStatementType) {
        this.config = requireNonNull(config);
        this.extensionMethod = extensionMethod;
        this.jdbiStatementType = jdbiStatementType;
    }

    /**
     * Inspect the type of the statement that owns this statement context.
     */
    @Beta
    public Type getJdbiStatementType() {
        return jdbiStatementType;
    }

    /**
     * Returns the type of the statement that owns this statement context as a descriptive string.
     *
     * @return the type of the statement that owns this statement context as a descriptive string
     */
    @Beta
    public String describeJdbiStatementType() {
        if (jdbiStatementType instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        return jdbiStatementType.getTypeName();
    }

    /**
     * Gets the configuration object of the given type, associated with this context.
     *
     * @param configClass the configuration type
     * @param <C>         the configuration type
     * @return the configuration object of the given type, associated with this context.
     */
    public <C extends JdbiConfig<C>> C getConfig(Class<C> configClass) {
        return config.get(configClass);
    }

    /**
     * Returns the {@code ConfigRegistry}.
     *
     * @return the {@code ConfigRegistry} used by this context.
     */
    public ConfigRegistry getConfig() {
        return config;
    }

    /**
     * Returns the attributes applied in this context.
     *
     * @return the defined attributes.
     */
    public Map<String, Object> getAttributes() {
        return getConfig(SqlStatements.class).getAttributes();
    }

    /**
     * Obtain the value of an attribute
     *
     * @param key the name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String key) {
        return getConfig(SqlStatements.class).getAttribute(key);
    }

    /**
     * Define an attribute for in this context.
     *
     * @param key   the key for the attribute
     * @param value the value for the attribute
     */
    public void define(String key, Object value) {
        getConfig(SqlStatements.class).define(key, value);
    }

    /**
     * Obtain an argument for given value in this context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findArgumentFor(Type type, Object value) {
        return getConfig(Arguments.class).findFor(type, value);
    }

    /**
     * Obtain an argument for given value in this context
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findArgumentFor(QualifiedType<?> type, Object value) {
        return getConfig(Arguments.class).findFor(type, value);
    }

    /**
     * Returns the strategy used by this context to bind array-type arguments to SQL statements.
     *
     * @return the strategy used to bind array-type arguments to SQL statements
     */
    public SqlArrayArgumentStrategy getSqlArrayArgumentStrategy() {
        return getConfig(SqlArrayTypes.class).getArgumentStrategy();
    }

    /**
     * Obtain an {@link SqlArrayType} for the given array element type in this context
     *
     * @param elementType the array element type.
     * @return an {@link SqlArrayType} for the given element type.
     */
    public Optional<SqlArrayType<?>> findSqlArrayTypeFor(Type elementType) {
        return getConfig(SqlArrayTypes.class).findFor(elementType);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findMapperFor(Class<T> type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findMapperFor(GenericType<T> type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a mapper for the given type, or empty if no row or column mappers
     * is registered for the given type.
     */
    public Optional<RowMapper<?>> findMapperFor(Type type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a mapper for the given qualified type in this context.
     *
     * @param type the target qualified type to map to
     * @return a mapper for the given qualified type, or empty if no row or column mappers
     * is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findMapperFor(QualifiedType<T> type) {
        return getConfig(Mappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(Class<T> type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(GenericType<T> type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a column mapper for the given qualified type in this context.
     *
     * @param type the qualified target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(QualifiedType<T> type) {
        return getConfig(ColumnMappers.class).findFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findRowMapperFor(Type type) {
        return getConfig(RowMappers.class).findFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findRowMapperFor(Class<T> type) {
        return getConfig(RowMappers.class).findFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param <T> the type to map
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findRowMapperFor(GenericType<T> type) {
        return getConfig(RowMappers.class).findFor(type);
    }

    /**
     * Obtain a collector for the given type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty null if no collector is registered for the given type.
     */
    public Optional<Collector<?, ?, ?>> findCollectorFor(Type containerType) {
        return getConfig(JdbiCollectors.class).findFor(containerType);
    }

    /**
     * Returns the element type for the given container type.
     *
     * @param containerType the container type.
     * @return the element type for the given container type, if available.
     */
    public Optional<Type> findElementTypeFor(Type containerType) {
        return getConfig(JdbiCollectors.class).findElementTypeFor(containerType);
    }

    StatementContext setRawSql(String rawSql) {
        this.rawSql = rawSql;
        return this;
    }

    /**
     * Obtain the initial sql for the statement used to create the statement
     *
     * @return the initial sql
     */
    public String getRawSql() {
        return rawSql;
    }

    void setRenderedSql(String renderedSql) {
        this.renderedSql = renderedSql;
    }

    /**
     * Obtain the rendered SQL statement
     * <p>
     * Not available until until statement execution time
     * </p>
     *
     * @return the sql statement after processing template directives.
     */
    public String getRenderedSql() {
        return renderedSql;
    }

    void setParsedSql(ParsedSql parsedSql) {
        this.parsedSql = parsedSql;
    }

    /**
     * Obtain the parsed SQL statement
     * <p>
     * Not available until until statement execution time
     * </p>
     *
     * @return the sql statement as it will be executed against the database
     */
    public ParsedSql getParsedSql() {
        return parsedSql;
    }

    void setStatement(PreparedStatement stmt) {
        statement = stmt;
    }

    /**
     * Obtain the actual prepared statement being used.
     * <p>
     * Not available until execution time
     * </p>
     *
     * @return Obtain the actual prepared statement being used.
     */
    public PreparedStatement getStatement() {
        return statement;
    }

    StatementContext setConnection(Connection connection) {
        this.connection = connection;
        return this;
    }

    /**
     * Obtain the JDBC connection being used for this statement
     *
     * @return the JDBC connection
     */
    public Connection getConnection() {
        return connection;
    }

    StatementContext setBinding(Binding b) {
        this.binding = b;
        return this;
    }

    /**
     * Returns the statement binding.
     *
     * @return The statement binding.
     */
    public Binding getBinding() {
        return binding;
    }

    /**
     * Sets whether the current statement returns generated keys.
     * @param returningGeneratedKeys return generated keys?
     */
    public void setReturningGeneratedKeys(boolean returningGeneratedKeys) {
        if (isConcurrentUpdatable() && returningGeneratedKeys) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent updatable and is returning generated keys.");
        }
        this.returningGeneratedKeys = returningGeneratedKeys;
    }

    /**
     * Whether the statement being generated is expected to return generated keys.
     *
     * @return whether the statement being generated is expected to return generated keys.
     */
    public boolean isReturningGeneratedKeys() {
        return returningGeneratedKeys || generatedKeysColumnNames.length > 0;
    }

    /**
     * Returns the generated key column names or empty if none were generated.
     *
     * @return the generated key column names. Returns an empty array if none exist.
     */
    public String[] getGeneratedKeysColumnNames() {
        return Arrays.copyOf(generatedKeysColumnNames, generatedKeysColumnNames.length);
    }

    /**
     * Set the generated key column names.
     * @param generatedKeysColumnNames the generated key column names
     */
    public void setGeneratedKeysColumnNames(String[] generatedKeysColumnNames) {
        this.generatedKeysColumnNames = Arrays.copyOf(generatedKeysColumnNames, generatedKeysColumnNames.length);
    }

    /**
     * Return if the statement should be concurrent updatable.
     *
     * If this returns true, the concurrency level of the created ResultSet will be
     * {@link java.sql.ResultSet#CONCUR_UPDATABLE}, otherwise the result set is not updatable,
     * and will have concurrency level {@link java.sql.ResultSet#CONCUR_READ_ONLY}.
     *
     * @return if the statement generated should be concurrent updatable.
     */
    public boolean isConcurrentUpdatable() {
        return concurrentUpdatable;
    }

    /**
     * Set the context to create a concurrent updatable result set.
     *
     * This cannot be combined with {@link #isReturningGeneratedKeys()}, only
     * one option may be selected. It does not make sense to combine these either, as one
     * applies to queries, and the other applies to updates.
     *
     * @param concurrentUpdatable if the result set should be concurrent updatable.
     */
    public void setConcurrentUpdatable(final boolean concurrentUpdatable) {
        if (concurrentUpdatable && isReturningGeneratedKeys()) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent "
                    + "updatable and is returning generated keys.");
        }
        this.concurrentUpdatable = concurrentUpdatable;
    }

    /**
     * Returns the query execution start as an {@link Instant}.
     *
     * @return the {@link Instant} at which query execution began
     */
    @Nullable
    public Instant getExecutionMoment() {
        return executionMoment;
    }

    /**
     * Sets the query execution start. This is not part of the Jdbi API and should not be called by
     * code outside JDBI.
     *
     * @param executionMoment Sets the start of query execution.
     */
    public void setExecutionMoment(Instant executionMoment) {
        this.executionMoment = executionMoment;
    }

    /**
     * If query execution was successful, returns the query execution end as an {@link Instant}.
     *
     * @return the {@link Instant} at which query execution ended, if it did so successfully
     */
    @Nullable
    public Instant getCompletionMoment() {
        return completionMoment;
    }

    /**
     * Sets the query execution end. This is not part of the Jdbi API and should not be called by
     * code outside JDBI.
     *
     * @param completionMoment Sets the end of query execution.
     */
    public void setCompletionMoment(Instant completionMoment) {
        this.completionMoment = completionMoment;
    }

    /**
     * If query execution failed, returns the query execution end as an {@link Instant}.
     *
     * @return the {@link Instant} at which query execution ended, if it did so with an exception
     */
    @Nullable
    public Instant getExceptionMoment() {
        return exceptionMoment;
    }

    /**
     * Sets the query execution end. This is not part of the Jdbi API and should not be called by
     * code outside JDBI.
     *
     * @param exceptionMoment Sets the end of query execution.
     */
    public void setExceptionMoment(Instant exceptionMoment) {
        this.exceptionMoment = exceptionMoment;
    }

    /**
     * Retrieve the number of mapped rows. Only intended for internal instrumentation to call.
     */
    @Alpha
    public long getMappedRows() {
        return mappedRows;
    }

    /**
     * Instrument the number of mapped rows. Only intended for internal instrumentation to call.
     */
    @Alpha
    public void setMappedRows(final long mappedRows) {
        this.mappedRows = mappedRows;
    }

    /**
     * Instrument the telemetry trace id. Only intended for internal instrumentation to call.
     */
    @Alpha
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    /**
     * Instrument the telemetry trace id. Only intended for internal instrumentation to call.
     */
    @Alpha
    public String getTraceId() {
        return traceId;
    }

    /**
     * Convenience method to measure elapsed time between start of query execution and completion or exception as appropriate. Do not call with a null argument or before a query has executed/exploded.
     * @param unit the time unit to convert to
     * @return the elapsed time in the given unit
     */
    public long getElapsedTime(ChronoUnit unit) {
        return unit.between(executionMoment, completionMoment == null ? exceptionMoment : completionMoment);
    }

    /**
     * Registers a {@code Cleanable} to be invoked when the statement context is closed. Cleanables can be registered
     * on a statement context, which will be cleaned up when
     * the statement finishes or (in the case of a ResultIterator), the object representing the results is closed.
     * <p>
     * Resources cleaned up by Jdbi include {@link ResultSet}, {@link Statement}, {@link Handle},
     * {@link java.sql.Array}, and {@link StatementBuilder}.
     *
     * @param cleanable the Cleanable to clean on close
     */
    public void addCleanable(Cleanable cleanable) {

        synchronized (cleanables) {
            cleanables.add(cleanable);
        }

        notifyCleanableAdded(cleanable);
    }

    @Override
    public void close() {

        try {
            List<Cleanable> cleanablesCopy;

            synchronized (cleanables) {
                if (cleanables.isEmpty()) {
                    return; // only notify that the context was cleaned.
                }

                cleanablesCopy = new ArrayList<>(cleanables);
                cleanables.clear();
            }

            Collections.reverse(cleanablesCopy);
            cleanablesCopy.forEach(this::notifyCleanableRemoved);

            ThrowableSuppressor throwableSuppressor = new ThrowableSuppressor();

            for (Cleanable cleanable : cleanablesCopy) {
                throwableSuppressor.suppressAppend(cleanable::close);
            }

            throwableSuppressor.throwIfNecessary(t -> new CloseException("Exception thrown while cleaning StatementContext", t));
        } finally {
            notifyContextCleaned();
        }
    }

    public ExtensionMethod getExtensionMethod() {
        return extensionMethod;
    }

    boolean isClean() {
        synchronized (cleanables) {
            return cleanables.isEmpty();
        }
    }

    private Collection<StatementContextListener> getListeners() {
        return getConfig(SqlStatements.class).getContextListeners();
    }

    private void notifyContextCreated() {
        Collection<StatementContextListener> listeners = getListeners();
        listeners.forEach(customizer -> customizer.contextCreated(this));
    }

    private void notifyContextCleaned() {
        Collection<StatementContextListener> listeners = getListeners();
        listeners.forEach(customizer -> customizer.contextCleaned(this));
    }

    private void notifyCleanableRemoved(Cleanable cleanable) {
        Collection<StatementContextListener> listeners = getListeners();
        listeners.forEach(customizer -> customizer.cleanableRemoved(this, cleanable));
    }

    private void notifyCleanableAdded(Cleanable cleanable) {
        Collection<StatementContextListener> listeners = getListeners();
        listeners.forEach(customizer -> customizer.cleanableAdded(this, cleanable));
    }
}

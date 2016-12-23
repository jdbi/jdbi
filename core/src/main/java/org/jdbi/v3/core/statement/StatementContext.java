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

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collector;

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
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;

/**
 * The statement context provides access to statement-local configuration.
 * Context is inherited from the parent {@link Handle} initially and changes will
 * not outlive the statement.
 * The context will be passed through most major jdbi APIs.
 *
 * DISCLAIMER: The class is not intended to be extended. The final modifier is absent to allow
 * mock tools to create a mock object of this class in the user code.
 */
public class StatementContext implements Closeable
{
    private final ConfigRegistry config;
    private final ExtensionMethod extensionMethod;

    private final Set<Cleanable> cleanables = new LinkedHashSet<>();

    private String            rawSql;
    private String            rewrittenSql;
    private PreparedStatement statement;
    private Connection        connection;
    private Binding           binding = new Binding();

    StatementContext() {
        this(new ConfigRegistry());
    }

    StatementContext(ConfigRegistry config)
    {
        this(config, null);
    }

    StatementContext(ConfigRegistry config, ExtensionMethod extensionMethod)
    {
        this.config = requireNonNull(config);
        this.extensionMethod = extensionMethod;
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
     * @return the {@code ConfigRegistry} this context owns
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
        return getConfig(Arguments.class).findFor(type, value, getConfig());
    }

    /**
     * Returns the strategy used to bind array-type arguments to SQL statements.
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
        return getConfig(SqlArrayTypes.class).findFor(elementType, getConfig());
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(Class<T> type) {
        return getConfig(ColumnMappers.class).findFor(type, config);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public <T> Optional<ColumnMapper<T>> findColumnMapperFor(GenericType<T> type) {
        return getConfig(ColumnMappers.class).findFor(type, getConfig());
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type) {
        return getConfig(ColumnMappers.class).findFor(type, getConfig());
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findRowMapperFor(Type type) {
        return getConfig(RowMappers.class).findFor(type, getConfig());
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findRowMapperFor(Class<T> type) {
        return getConfig(RowMappers.class).findFor(type, getConfig());
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public <T> Optional<RowMapper<T>> findRowMapperFor(GenericType<T> type) {
        return getConfig(RowMappers.class).findFor(type, getConfig());
    }

    /**
     * Obtain a collector for the given type.
     *
     * @param containerType the container type.
     * @return a Collector for the given container type, or empty null if no collector is registered for the given type.
     */
    public Optional<Collector<?,?,?>> findCollectorFor(Type containerType) {
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

    StatementContext setRawSql(String rawSql)
    {
        this.rawSql = rawSql;
        return this;
    }

    /**
     * Obtain the initial sql for the statement used to create the statement
     *
     * @return the initial sql
     */
    public String getRawSql()
    {
        return rawSql;
    }

    void setRewrittenSql(String rewrittenSql)
    {
        this.rewrittenSql = rewrittenSql;
    }

    /**
     * Obtain the located and rewritten sql
     * <p>
     * Not available until until statement execution time
     * </p>
     *
     * @return the sql as it will be executed against the database
     */
    public String getRewrittenSql()
    {
        return rewrittenSql;
    }

    void setStatement(PreparedStatement stmt)
    {
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
    public PreparedStatement getStatement()
    {
        return statement;
    }

    StatementContext setConnection(Connection connection)
    {
        this.connection = connection;
        return this;
    }

    /**
     * Obtain the JDBC connection being used for this statement
     *
     * @return the JDBC connection
     */
    public Connection getConnection()
    {
        return connection;
    }

    StatementContext setBinding(Binding b)
    {
        this.binding = b;
        return this;
    }

    public Binding getBinding()
    {
        return binding;
    }

    /**
     * Registers a {@code Cleanable} to be invoked when the statement context is closed. Cleanables can be registered
     * on a statement context, which will be cleaned up when
     * the statement finishes or (in the case of a ResultIterator), the object representing the results is closed.
     * <p>
     * Resources cleaned up by JDBI include {@link ResultSet}, {@link Statement}, {@link Handle},
     * {@link java.sql.Array}, and {@link StatementBuilder}.
     */
    public void addCleanable(Cleanable cleanable) {
        cleanables.add(cleanable);
    }

    @Override
    public void close()
    {
        SQLException exception = null;
        try {
            List<Cleanable> cleanables = new ArrayList<>(this.cleanables);
            this.cleanables.clear();
            Collections.reverse(cleanables);
            for (Cleanable cleanable : cleanables) {
                try {
                    cleanable.close();
                } catch (SQLException e) {
                    if (exception == null) {
                        exception = e;
                    } else {
                        exception.addSuppressed(e);
                    }
                }
            }
        } finally {
            if (exception != null) {
                throw new CloseException("Exception thrown while cleaning StatementContext", exception);
            }
        }
    }

    public ExtensionMethod getExtensionMethod()
    {
        return extensionMethod;
    }
}

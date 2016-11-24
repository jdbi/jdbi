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

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.array.SqlArrayArgumentStrategy;
import org.jdbi.v3.core.array.SqlArrayType;
import org.jdbi.v3.core.array.SqlArrayTypes;
import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.SqlStatements;

/**
 * The statement context provides a means for passing client specific information through the
 * evaluation of a statement. The context is not used by jDBI internally, but will be passed
 * to all statement customizers. This makes it possible to parameterize the processing of
 * the tweakable parts of the statement processing cycle.
 *
 * DISCLAIMER: The class is not intended to be extended. The final modifier is absent to allow
 * mock tools to create a mock object of this class in the user code.
 */
public class StatementContext implements Closeable
{
    private final ConfigRegistry config;
    private final ExtensionMethod extensionMethod;

    private final Cleanables cleanables = new Cleanables();

    private String            rawSql;
    private String            rewrittenSql;
    private PreparedStatement statement;
    private Connection        connection;
    private Binding           binding;
    private boolean           returningGeneratedKeys;
    private boolean           concurrentUpdatable;
    private String[]          generatedKeysColumnNames;

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
        return config.findArgumentFor(type, value);
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
        return config.findSqlArrayTypeFor(elementType);
    }

    /**
     * Obtain a column mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a ColumnMapper for the given type, or empty if no column mapper is registered for the given type.
     */
    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type) {
        return config.findColumnMapperFor(type);
    }

    /**
     * Obtain a row mapper for the given type in this context.
     *
     * @param type the target type to map to
     * @return a RowMapper for the given type, or empty if no row mapper is registered for the given type.
     */
    public Optional<RowMapper<?>> findRowMapperFor(Type type) {
        return config.findRowMapperFor(type);
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

    void setRawSql(String rawSql)
    {
        this.rawSql = rawSql;
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

    void setConnection(Connection connection)
    {
        this.connection = connection;
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

    void setBinding(Binding b)
    {
        this.binding = b;
    }

    public Binding getBinding()
    {
        return binding;
    }

    Cleanables getCleanables()
    {
        return cleanables;
    }

    public void addCleanable(Cleanable cleanable) {
        getCleanables().add(cleanable);
    }

    @Override
    public void close()
    {
        cleanables.close();
    }

    public ExtensionMethod getExtensionMethod()
    {
        return extensionMethod;
    }

    void setReturningGeneratedKeys(boolean b)
    {
        if (isConcurrentUpdatable() && b) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent "
                    + "updatable and is returning generated keys.");
        }
        this.returningGeneratedKeys = b;
    }

    /**
     * @return whether the statement being generated is expected to return generated keys.
     */
    public boolean isReturningGeneratedKeys()
    {
        return returningGeneratedKeys || generatedKeysColumnNames != null && generatedKeysColumnNames.length > 0;
    }

    public String[] getGeneratedKeysColumnNames()
    {
        if (generatedKeysColumnNames == null) {
            return new String[0];
        }
        return Arrays.copyOf(generatedKeysColumnNames, generatedKeysColumnNames.length);
    }

    void setGeneratedKeysColumnNames(String[] generatedKeysColumnNames)
    {
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
    void setConcurrentUpdatable(final boolean concurrentUpdatable) {
        if (concurrentUpdatable && isReturningGeneratedKeys()) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent "
                    + "updatable and is returning generated keys.");
        }
        this.concurrentUpdatable = concurrentUpdatable;
    }
}

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

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;

/**
 * An implementation of {@link StatementContext} which holds its state and provides a means
 * only for {@link BaseStatement}'s implementations to alter it. Doing so, we don't expose
 * the internal state of the context to the user, so he/she can change it only via public
 * API of {@link Query} or {@link Update}.
 * <p>
 * This also make {@link StatementContext} more testable, because components which interact
 * with it, can now easily create mock implementations of it.
 */
public final class ConcreteStatementContext implements StatementContext
{
    private final JdbiConfig config;
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

    public ConcreteStatementContext() {
        this(new JdbiConfig());
    }

    ConcreteStatementContext(JdbiConfig config)
    {
        this(config, null);
    }

    ConcreteStatementContext(JdbiConfig config, ExtensionMethod extensionMethod)
    {
        this.config = requireNonNull(config);
        this.extensionMethod = extensionMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object setAttribute(String key, Object value)
    {
        return config.statementAttributes.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getAttribute(String key)
    {
        return config.statementAttributes.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> getAttributes()
    {
        return config.statementAttributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ColumnMapper<?>> findColumnMapperFor(Type type)
    {
        return config.mappingRegistry.findColumnMapperFor(type, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<RowMapper<?>> findRowMapperFor(Type type)
    {
        return config.mappingRegistry.findRowMapperFor(type, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Argument> findArgumentFor(Type type, Object value)
    {
        return config.argumentRegistry.findArgumentFor(type, value, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Collector<?, ?, ?>> findCollectorFor(Type type)
    {
        return config.collectorRegistry.findCollectorFor(type);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Type> elementTypeFor(Type containerType)
    {
        return config.collectorRegistry.elementTypeFor(containerType);
    }

    void setRawSql(String rawSql)
    {
        this.rawSql = rawSql;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRawSql()
    {
        return rawSql;
    }

    void setRewrittenSql(String rewrittenSql)
    {
        this.rewrittenSql = rewrittenSql;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRewrittenSql()
    {
        return rewrittenSql;
    }

    void setStatement(PreparedStatement stmt)
    {
        statement = stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PreparedStatement getStatement()
    {
        return statement;
    }

    void setConnection(Connection connection)
    {
        this.connection = connection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Connection getConnection()
    {
        return connection;
    }

    void setBinding(Binding b)
    {
        this.binding = b;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Binding getBinding()
    {
        return binding;
    }

    Cleanables getCleanables() {
        return cleanables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public boolean isReturningGeneratedKeys()
    {
        return returningGeneratedKeys || generatedKeysColumnNames != null && generatedKeysColumnNames.length > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
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
    void setConcurrentUpdatable(final boolean concurrentUpdatable)
    {
        if (concurrentUpdatable && isReturningGeneratedKeys()) {
            throw new IllegalArgumentException("Cannot create a result set that is concurrent "
                    + "updatable and is returning generated keys.");
        }
        this.concurrentUpdatable = concurrentUpdatable;
    }
}

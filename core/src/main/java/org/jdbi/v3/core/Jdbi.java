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


import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.SqlArrayType;
import org.jdbi.v3.core.argument.SqlArrayTypeFactory;
import org.jdbi.v3.core.collector.CollectorFactory;
import org.jdbi.v3.core.exception.UnableToObtainConnectionException;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConfig;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.NoSuchExtensionException;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.rewriter.StatementRewriter;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.StatementBuilderFactory;
import org.jdbi.v3.core.transaction.LocalTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionCallback;
import org.jdbi.v3.core.transaction.TransactionConsumer;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class  provides the access point for jDBI. Use it to obtain Handle instances
 * and provide "global" configuration for all handles obtained from it.
 */
public class Jdbi
{
    private static final Logger LOG = LoggerFactory.getLogger(Jdbi.class);

    private final JdbiConfig config = new JdbiConfig();

    private final ConnectionFactory connectionFactory;
    private final AtomicReference<TransactionHandler> transactionhandler = new AtomicReference<>(new LocalTransactionHandler());
    private final AtomicReference<StatementBuilderFactory> statementBuilderFactory = new AtomicReference<>(new DefaultStatementBuilderFactory());

    private final CopyOnWriteArrayList<JdbiPlugin> plugins = new CopyOnWriteArrayList<>();

    private Jdbi(ConnectionFactory connectionFactory)
    {
        Objects.requireNonNull(connectionFactory, "null connectionFactory");
        this.connectionFactory = connectionFactory;

        baseConfiguration();
    }

    private void baseConfiguration()
    {
        configure(SqlArrayConfiguration.class, c -> {});
    }

    /**
     * @param dataSource the data source.
     *
     * @return a DBI which uses the given data source as a connection factory.
     */
    public static Jdbi create(DataSource dataSource)
    {
        return create(dataSource::getConnection);
    }

    /**
     * Factory used to allow for obtaining a Connection in a customized manner.
     *
     * <p>
     * The {@link ConnectionFactory#openConnection()} method will be invoked to obtain a connection instance
     * whenever a Handle is opened.
     * </p>
     *
     * @param connectionFactory Provides JDBC connections to Handle instances
     *
     * @return a DBI which uses the given connection factory.
     */
    public static Jdbi create(ConnectionFactory connectionFactory) {
        return new Jdbi(connectionFactory);
    }

    /**
     * @param url JDBC URL for connections
     *
     * @return a DBI which uses {@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url)
    {
        Objects.requireNonNull(url, "null url");
        return create(() -> DriverManager.getConnection(url));
    }

    /**
     * @param url   JDBC URL for connections
     * @param properties Properties to pass to DriverManager.getConnection(url, props) for each new handle
     *
     * @return a DBI which uses {@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url, final Properties properties)
    {
        Objects.requireNonNull(url, "null url");
        Objects.requireNonNull(properties, "null properties");
        return create(() -> DriverManager.getConnection(url, properties));
    }

    /**
     * @param url      JDBC URL for connections
     * @param username User name for connection authentication
     * @param password Password for connection authentication
     *
     * @return a DBI which uses {@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url, final String username, final String password)
    {
        Objects.requireNonNull(url, "null url");
        Objects.requireNonNull(username, "null username");
        Objects.requireNonNull(password, "null password");
        return create(() -> DriverManager.getConnection(url, username, password));
    }

    /**
     * Convenience method used to obtain a handle from a specific data source
     *
     * @param dataSource the JDBC data source.
     *
     * @return Handle using a Connection obtained from the provided DataSource
     */
    public static Handle open(DataSource dataSource)
    {
        return create(dataSource).open();
    }

    /**
     * Create a Handle wrapping a particular JDBC Connection
     *
     * @param connection the JDBC connection
     *
     * @return Handle bound to connection
     */
    public static Handle open(final Connection connection)
    {
        Objects.requireNonNull(connection, "null connection");
        return create(() -> connection).open();
    }

    /**
     * Obtain a handle with just a JDBC URL
     *
     * @param url JDBC Url
     *
     * @return newly opened Handle
     */
    public static Handle open(final String url)
    {
        return create(url).open();
    }

    /**
     * Obtain a handle with just a JDBC URL
     *
     * @param url      JDBC Url
     * @param username JDBC username for authentication
     * @param password JDBC password for authentication
     *
     * @return newly opened Handle
     */
    public static Handle open(final String url, final String username, final String password)
    {
        return create(url, username, password).open();
    }

    /**
     * Obtain a handle with just a JDBC URL
     *
     * @param url   JDBC Url
     * @param props JDBC properties
     *
     * @return newly opened Handle
     */
    public static Handle open(final String url, final Properties props)
    {
        return create(url, props).open();
    }

    public Jdbi installPlugins()
    {
        ServiceLoader.load(JdbiPlugin.class).forEach(this::installPlugin);
        LOG.debug("Automatically installed plugins {}", plugins);
        return this;
    }

    public Jdbi installPlugin(JdbiPlugin plugin)
    {
        plugin.customizeDbi(this);
        plugins.add(plugin);
        return this;
    }

    /**
     * Allows customization of how prepared statements are created. When a Handle is created
     * against this DBI instance the factory will be used to create a StatementBuilder for
     * that specific handle. When the handle is closed, the StatementBuilder's close method
     * will be invoked.
     *
     * @param factory the new statement builder factory.
     * @return this
     */
    public Jdbi setStatementBuilderFactory(StatementBuilderFactory factory)
    {
        this.statementBuilderFactory.set(factory);
        return this;
    }

    public StatementBuilderFactory getStatementBuilderFactory()
    {
        return this.statementBuilderFactory.get();
    }

    /**
     * Use a non-standard StatementRewriter to transform SQL for all Handle instances
     * created by this DBI.
     *
     * @param rewriter StatementRewriter to use on all Handle instances
     * @return this
     */
    public Jdbi setStatementRewriter(StatementRewriter rewriter)
    {
        Objects.requireNonNull(rewriter, "null statement rewriter");
        config.statementRewriter = rewriter;
        return this;
    }

    public StatementRewriter getStatementRewriter()
    {
        return config.statementRewriter;
    }

    /**
     * Specify the TransactionHandler instance to use. This allows overriding
     * transaction semantics, or mapping into different transaction
     * management systems.
     * <p>
     * The default version uses local transactions on the database Connection
     * instances obtained.
     * </p>
     *
     * @param handler The TransactionHandler to use for all Handle instances obtained
     *                from this DBI
     * @return this
     */
    public Jdbi setTransactionHandler(TransactionHandler handler)
    {
        Objects.requireNonNull(handler, "null transaction handler");
        this.transactionhandler.set(handler);
        return this;
    }

    public TransactionHandler getTransactionHandler()
    {
        return this.transactionhandler.get();
    }

    /**
     * Add a callback to accumulate timing information about the queries running from this
     * data source.
     *
     * @param timingCollector the new timing collector
     * @return this
     */
    public Jdbi setTimingCollector(final TimingCollector timingCollector) {
        if (timingCollector == null) {
            config.timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
        }
        else {
            config.timingCollector = timingCollector;
        }
        return this;
    }

    public TimingCollector getTimingCollector()
    {
        return config.timingCollector;
    }

    public Jdbi registerArgumentFactory(ArgumentFactory argumentFactory)
    {
        config.argumentRegistry.register(argumentFactory);
        return this;
    }

    /**
     * Register an array element type that is supported by the JDBC vendor.
     *
     * @param elementType the array element type
     * @param sqlTypeName the vendor-specific SQL type name for the array type.  This value will be passed to
     *                    {@link java.sql.Connection#createArrayOf(String, Object[])} to create SQL arrays.
     * @return this
     */
    public Jdbi registerArrayType(Class<?> elementType, String sqlTypeName)
    {
        config.argumentRegistry.registerArrayType(elementType, sqlTypeName);
        return this;
    }

    /**
     * Register a {@link SqlArrayType} which will have its parameterized type inspected to determine which element type
     * it supports. {@link SqlArrayType SQL array types} are used to convert array-like arguments into SQL arrays.
     * <p>
     * The parameter must be concretely parameterized; we use the type argument {@code T} to determine if it applies to
     * a given element type.
     *
     * @param arrayType the {@link SqlArrayType}
     * @return this
     * @throws UnsupportedOperationException if the argument is not a concretely parameterized type
     */
    public Jdbi registerArrayType(SqlArrayType<?> arrayType)
    {
        config.argumentRegistry.registerArrayType(arrayType);
        return this;
    }

    /**
     * Register a {@link SqlArrayTypeFactory}. A factory is provided element types and, if it supports it, provides an
     * {@link SqlArrayType} for it.
     *
     * @param factory the factory
     * @return this
     */
    public Jdbi registerArrayType(SqlArrayTypeFactory factory)
    {
        config.argumentRegistry.registerArrayType(factory);
        return this;
    }

    public Jdbi registerCollectorFactory(CollectorFactory collectorFactory)
    {
        config.collectorRegistry.register(collectorFactory);
        return this;
    }

    public Jdbi registerExtension(ExtensionFactory<?> extensionFactory)
    {
        config.extensionRegistry.register(extensionFactory);
        return this;
    }

    public <C extends ExtensionConfig<C>> Jdbi configureExtension(Class<C> configClass, Consumer<C> consumer)
    {
        config.extensionRegistry.configure(configClass, consumer);
        return this;
    }

    public <C extends ExtensionConfig<C>> Jdbi configure(Class<C> configClass, Consumer<C> consumer)
    {
        registerExtension(new ConfigOnlyExtension<C>(configClass));
        return configureExtension(configClass, consumer);
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param mapper the column mapper
     * @return this
     */
    public Jdbi registerColumnMapper(ColumnMapper<?> mapper) {
        config.mappingRegistry.addColumnMapper(mapper);
        return this;
    }

    /**
     * Register a column mapper factory.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     *
     * @param factory the column mapper factory
     * @return this
     */
    public Jdbi registerColumnMapper(ColumnMapperFactory factory) {
        config.mappingRegistry.addColumnMapper(factory);
        return this;
    }

    /**
     * Register a row mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param mapper the row mapper
     * @return this
     */
    public Jdbi registerRowMapper(RowMapper<?> mapper) {
        config.mappingRegistry.addRowMapper(mapper);
        return this;
    }

    /**
     * Register a row mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     *
     * @param factory the row mapper factory
     * @return this
     */
    public Jdbi registerRowMapper(RowMapperFactory factory) {
        config.mappingRegistry.addRowMapper(factory);
        return this;
    }

    /**
     * Define an attribute on every {@link StatementContext} for every statement created
     * from a handle obtained from this DBI instance.
     *
     * @param key   The key for the attribute
     * @param value the value for the attribute
     * @return this
     */
    public Jdbi define(String key, Object value)
    {
        config.statementAttributes.put(key, value);
        return this;
    }

    /**
     * Obtain a Handle to the data source wrapped by this DBI instance
     *
     * @return an open Handle instance
     */
    public Handle open()
    {
        try {
            final long start = System.nanoTime();
            Connection conn = connectionFactory.openConnection();
            final long stop = System.nanoTime();

            for (JdbiPlugin p : plugins) {
                conn = p.customizeConnection(conn);
            }

            StatementBuilder cache = statementBuilderFactory.get().createStatementBuilder(conn);
            Handle h = new Handle(JdbiConfig.copyOf(config), transactionhandler.get(), cache, conn);
            for (JdbiPlugin p : plugins) {
                h = p.customizeHandle(h);
            }
            LOG.trace("DBI [{}] obtain handle [{}] in {}ms", this, h, (stop - start) / 1000000L);
            return h;
        }
        catch (SQLException e) {
            throw new UnableToObtainConnectionException(e);
        }
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return the value returned by callback
     *
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X
    {
        try (Handle h = this.open()) {
            return callback.withHandle(h);
        }
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useHandle(final HandleConsumer<X> callback) throws X
    {
        withHandle(h -> { callback.useHandle(h); return null; });
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return the value returned by callback
     *
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R inTransaction(final TransactionCallback<R, X> callback) throws X
    {
        return withHandle(handle -> handle.<R, X>inTransaction(callback));
    }

    public <X extends Exception> void useTransaction(final TransactionConsumer<X> callback) throws X
    {
        useHandle(handle -> handle.useTransaction(callback));
    }

    public <R, X extends Exception> R inTransaction(final TransactionIsolationLevel isolation, final TransactionCallback<R, X> callback) throws X
    {
        return withHandle(handle -> handle.<R, X>inTransaction(isolation, callback));
    }

    public <X extends Exception> void useTransaction(final TransactionIsolationLevel isolation, final TransactionConsumer<X> callback) throws X
    {
        useHandle(handle -> handle.useTransaction(isolation, callback));
    }

    /**
     * A convenience method which opens an extension of the given type, yields it to a callback, and returns the result
     * of the callback. A handle is opened if needed by the extension, and closed before returning to the caller.
     *
     * @param extensionType the type of extension.
     * @param callback      a callback which will receive the extension.
     * @param <R> the return type
     * @param <E> the extension type
     * @param <X> the exception type optionally thrown by the callback
     * @return the value returned by the callback.
     * @throws NoSuchExtensionException if no {@link ExtensionFactory} is registered which supports the given extension
     *                                  type.
     * @throws X                        if thrown by the callback.
     */
    public <R, E, X extends Exception> R withExtension(Class<E> extensionType, ExtensionCallback<R, E, X> callback)
            throws NoSuchExtensionException, X
    {
        try (LazyHandleSupplier handle = new LazyHandleSupplier(this)) {
            E extension = config.extensionRegistry.findExtensionFor(extensionType, handle)
                    .orElseThrow(() -> new NoSuchExtensionException("Extension not found: " + extensionType));

            return callback.withExtension(extension);
        }
    }

    /**
     * A convenience method which opens an extension of the given type, and yields it to a callback. A handle is opened
     * if needed by the extention, and closed before returning to the caller.
     *
     * @param extensionType the type of extension
     * @param callback      a callback which will receive the extension
     * @param <E>           the extension type
     * @param <X>           the exception type optionally thrown by the callback
     * @throws NoSuchExtensionException if no {@link ExtensionFactory} is registered which supports the given extension type.
     * @throws X                        if thrown by the callback.
     */
    public <E, X extends Exception> void useExtension(Class<E> extensionType, ExtensionConsumer<E, X> callback)
            throws NoSuchExtensionException, X {
        withExtension(extensionType, extension -> {
            callback.useExtension(extension);
            return null;
        });
    }

    /**
     * @param extensionType the type of extension. Must be a public interface type.
     * @param <E> the extension type
     *
     * @return an extension which opens and closes handles (as needed) for individual method calls. Only public
     * interface types may be used as on-demand extensions.
     */
    public <E> E onDemand(Class<E> extensionType) throws NoSuchExtensionException {
        if (!extensionType.isInterface()) {
            throw new IllegalArgumentException("On-demand extensions are only supported for interfaces.");
        }
        if (!Modifier.isPublic(extensionType.getModifiers())) {
            throw new IllegalArgumentException("On-demand extensions types must be public.");
        }
        if (!config.extensionRegistry.hasExtensionFor(extensionType)) {
            throw new NoSuchExtensionException("Extension not found: " + extensionType);
        }

        return OnDemandExtensions.create(this, extensionType);
    }
}

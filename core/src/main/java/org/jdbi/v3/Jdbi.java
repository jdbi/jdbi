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
package org.jdbi.v3;


import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.jdbi.v3.exceptions.UnableToObtainConnectionException;
import org.jdbi.v3.extension.ExtensionCallback;
import org.jdbi.v3.extension.ExtensionConfig;
import org.jdbi.v3.extension.ExtensionConsumer;
import org.jdbi.v3.extension.ExtensionFactory;
import org.jdbi.v3.extension.NoSuchExtensionException;
import org.jdbi.v3.spi.JdbiPlugin;
import org.jdbi.v3.tweak.ArgumentFactory;
import org.jdbi.v3.tweak.CollectorFactory;
import org.jdbi.v3.tweak.ConnectionFactory;
import org.jdbi.v3.tweak.ColumnMapper;
import org.jdbi.v3.tweak.RowMapper;
import org.jdbi.v3.tweak.StatementBuilder;
import org.jdbi.v3.tweak.StatementBuilderFactory;
import org.jdbi.v3.tweak.StatementLocator;
import org.jdbi.v3.tweak.StatementRewriter;
import org.jdbi.v3.tweak.TransactionHandler;
import org.jdbi.v3.tweak.transactions.LocalTransactionHandler;
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
        if (connectionFactory == null) {
            throw new IllegalArgumentException("null connectionFactory");
        }
        this.connectionFactory = connectionFactory;
    }

    /**
     * Constructor for use with a DataSource which will provide
     *
     * @param dataSource
     */
    public static Jdbi create(DataSource dataSource)
    {
        return create(dataSource::getConnection);
    }

    /**
     * Factory used to allow for obtaining a Connection in a customized manner.
     * <p/>
     * The {@link org.jdbi.v3.tweak.ConnectionFactory#openConnection()} method will
     * be invoked to obtain a connection instance whenever a Handle is opened.
     *
     * @param connectionFactory Prvides JDBC connections to Handle instances
     */
    public static Jdbi create(ConnectionFactory connectionFactory) {
        return new Jdbi(connectionFactory);
    }

    /**
     * Create a DBI which directly uses the DriverManager
     *
     * @param url JDBC URL for connections
     */
    public static Jdbi create(final String url)
    {
        if (url == null) {
            throw new IllegalArgumentException("null url");
        }
        return create(() -> DriverManager.getConnection(url));
    }

    /**
     * Create a DBI which directly uses the DriverManager
     *
     * @param url   JDBC URL for connections
     * @param properties Properties to pass to DriverManager.getConnection(url, props) for each new handle
     */
    public static Jdbi create(final String url, final Properties properties)
    {
        if (url == null) {
            throw new IllegalArgumentException("null url");
        }
        if (properties == null) {
            throw new IllegalArgumentException("null properties");
        }
        return create(() -> DriverManager.getConnection(url, properties));
    }

    /**
     * Create a DBI which directly uses the DriverManager
     *
     * @param url      JDBC URL for connections
     * @param username User name for connection authentication
     * @param password Password for connection authentication
     */
    public static Jdbi create(final String url, final String username, final String password)
    {
        if (url == null) {
            throw new IllegalArgumentException("null url");
        }
        if (username == null) {
            throw new IllegalArgumentException("null username");
        }
        if (password == null) {
            throw new IllegalArgumentException("null password");
        }
        return create(() -> DriverManager.getConnection(url, username, password));
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
     * Use a non-standard StatementLocator to look up named statements for all
     * handles created from this DBi instance.
     *
     * @param locator StatementLocator which will be used by all Handle instances
     *                created from this DBI
     */
    public void setStatementLocator(StatementLocator locator)
    {
        assert locator != null;
        config.statementLocator = locator;
    }

    public StatementLocator getStatementLocator()
    {
        return config.statementLocator;
    }

    /**
     * Use a non-standard StatementRewriter to transform SQL for all Handle instances
     * created by this DBI.
     *
     * @param rewriter StatementRewriter to use on all Handle instances
     */
    public void setStatementRewriter(StatementRewriter rewriter)
    {
        assert rewriter != null;
        config.statementRewriter = rewriter;
    }

    public StatementRewriter getStatementRewriter()
    {
        return config.statementRewriter;
    }

    /**
     * Specify the TransactionHandler instance to use. This allows overriding
     * transaction semantics, or mapping into different transaction
     * management systems.
     * <p/>
     * The default version uses local transactions on the database Connection
     * instances obtained.
     *
     * @param handler The TransactionHandler to use for all Handle instances obtained
     *                from this DBI
     */
    public void setTransactionHandler(TransactionHandler handler)
    {
        assert handler != null;
        this.transactionhandler.set(handler);
    }

    public TransactionHandler getTransactionHandler()
    {
        return this.transactionhandler.get();
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
            Handle h = new BasicHandle(JdbiConfig.copyOf(config), transactionhandler.get(), cache, conn);
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
     * Register a row mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     */
    public void registerRowMapper(RowMapper<?> mapper) {
        config.mappingRegistry.addRowMapper(mapper);
    }

    /**
     * Register a row mapper factory.
     *
     * Will be used with {@link Query#mapTo(Class)} for registered mappings.
     */
    public void registerRowMapper(RowMapperFactory factory) {
        config.mappingRegistry.addRowMapper(factory);
    }

    /**
     * Register a column mapper which will have its parameterized type inspected to determine what it maps to
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     */
    public void registerColumnMapper(ColumnMapper<?> mapper) {
        config.mappingRegistry.addColumnMapper(mapper);
    }

    /**
     * Register a column mapper factory.
     *
     * Column mappers may be reused by {@link RowMapper} to map individual columns.
     */
    public void registerColumnMapper(ColumnMapperFactory factory) {
        config.mappingRegistry.addColumnMapper(factory);
    }

    /**
     * Define an attribute on every {@link StatementContext} for every statement created
     * from a handle obtained from this DBI instance.
     *
     * @param key   The key for the attribute
     * @param value the value for the attribute
     */
    public void define(String key, Object value)
    {
        config.statementAttributes.put(key, value);
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param callback A callback which will receive an open Handle
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
        try (LazyHandle handle = new LazyHandle(this)) {
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
     * Returns an extension which opens and closes handles (as needed) for individual method calls. Only public
     * interface types may be used as on-demand extensions.
     *
     * @param extensionType the type of extension. Must be a public interface type.
     * @param <E> the extension type
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

    /**
     * Convenience method used to obtain a handle from a specific data source
     *
     * @param dataSource
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
     * @param connection
     *
     * @return Handle bound to connection
     */
    public static Handle open(final Connection connection)
    {
        if (connection == null) {
            throw new IllegalArgumentException("null connection");
        }
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

    /**
     * Allows customization of how prepared statements are created. When a Handle is created
     * against this DBI instance the factory will be used to create a StatementBuilder for
     * that specific handle. When the handle is closed, the StatementBuilder's close method
     * will be invoked.
     */
    public void setStatementBuilderFactory(StatementBuilderFactory factory)
    {
        this.statementBuilderFactory.set(factory);
    }

    public StatementBuilderFactory getStatementBuilderFactory()
    {
        return this.statementBuilderFactory.get();
    }

    /**
     * Add a callback to accumulate timing information about the queries running from this
     * data source.
     */
    public void setTimingCollector(final TimingCollector timingCollector) {
        if (timingCollector == null) {
            config.timingCollector = TimingCollector.NOP_TIMING_COLLECTOR;
        }
        else {
            config.timingCollector = timingCollector;
        }
    }

    public TimingCollector getTimingCollector()
    {
        return config.timingCollector;
    }

    public void registerArgumentFactory(ArgumentFactory argumentFactory)
    {
        config.argumentRegistry.register(argumentFactory);
    }

    public void registerCollectorFactory(CollectorFactory collectorFactory)
    {
        config.collectorRegistry.register(collectorFactory);
    }

    public void registerExtensionFactory(ExtensionFactory extensionFactory)
    {
        config.extensionRegistry.register(extensionFactory);
    }

    public <C extends ExtensionConfig<C>> void configureExtension(Class<C> configClass, Consumer<C> consumer) {
        config.extensionRegistry.configure(configClass, consumer);
    }
}

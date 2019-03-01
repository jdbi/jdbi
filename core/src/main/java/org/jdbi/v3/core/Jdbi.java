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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.NoSuchExtensionException;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.DefaultStatementBuilder;
import org.jdbi.v3.core.statement.StatementBuilder;
import org.jdbi.v3.core.statement.StatementBuilderFactory;
import org.jdbi.v3.core.transaction.LocalTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Main entry point; configurable wrapper around a JDBC {@link DataSource}.
 * Use it to obtain Handle instances and provide configuration
 * for all handles obtained from it.
 */
public class Jdbi implements Configurable<Jdbi> {
    private static final Logger LOG = LoggerFactory.getLogger(Jdbi.class);

    private final ConfigRegistry config = new ConfigRegistry();

    private final ConnectionFactory connectionFactory;
    private final AtomicReference<TransactionHandler> transactionhandler = new AtomicReference<>(new LocalTransactionHandler());
    private final AtomicReference<StatementBuilderFactory> statementBuilderFactory = new AtomicReference<>(DefaultStatementBuilder.FACTORY);

    private final CopyOnWriteArrayList<JdbiPlugin> plugins = new CopyOnWriteArrayList<>();

    private final ThreadLocal<Handle> threadHandle = new ThreadLocal<>();

    private Jdbi(ConnectionFactory connectionFactory) {
        Objects.requireNonNull(connectionFactory, "null connectionFactory");
        this.connectionFactory = connectionFactory;
    }

    /**
     * @param connection db connection
     *
     * @return a Jdbi which works on single connection
     */
    public static Jdbi create(Connection connection) {
        return create(new SingleConnectionFactory(connection));
    }

    /**
     * @param dataSource the data source.
     *
     * @return a Jdbi which uses the given data source as a connection factory.
     */
    public static Jdbi create(DataSource dataSource) {
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
     * @return a Jdbi which uses the given connection factory.
     */
    public static Jdbi create(ConnectionFactory connectionFactory) {
        return new Jdbi(connectionFactory);
    }

    /**
     * @param url JDBC URL for connections
     *
     * @return a Jdbi which uses {@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url) {
        Objects.requireNonNull(url, "null url");
        return create(() -> DriverManager.getConnection(url));
    }

    /**
     * @param url   JDBC URL for connections
     * @param properties Properties to pass to DriverManager.getConnection(url, props) for each new handle
     *
     * @return a Jdbi which uses {@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url, final Properties properties) {
        Objects.requireNonNull(url, "null url");
        Objects.requireNonNull(properties, "null properties");
        return create(() -> DriverManager.getConnection(url, properties));
    }

    /**
     * @param url      JDBC URL for connections
     * @param username User name for connection authentication
     * @param password Password for connection authentication
     *
     * @return a Jdbi which uses {@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url, final String username, final String password) {
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
    public static Handle open(DataSource dataSource) {
        return create(dataSource).open();
    }

    /**
     * Convenience method used to obtain a handle from a {@link ConnectionFactory}.
     *
     * @param connectionFactory the connection factory
     *
     * @return Handle using a Connection obtained from the provided connection factory
     */
    public static Handle open(ConnectionFactory connectionFactory) {
        return create(connectionFactory).open();
    }

    /**
     * Create a Handle wrapping a particular JDBC Connection
     *
     * @param connection the JDBC connection
     *
     * @return Handle bound to connection
     */
    public static Handle open(final Connection connection) {
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
    public static Handle open(final String url) {
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
    public static Handle open(final String url, final String username, final String password) {
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
    public static Handle open(final String url, final Properties props) {
        return create(url, props).open();
    }

    /**
     * Use the {@link ServiceLoader} API to detect and install plugins automagically.
     * Some people consider this feature dangerous; some consider it essential --
     * use at your own risk.
     * @return this
     */
    public Jdbi installPlugins() {
        ServiceLoader.load(JdbiPlugin.class).forEach(this::installPlugin);
        LOG.debug("Automatically installed plugins {}", plugins);
        return this;
    }

    /**
     * Install a given {@link JdbiPlugin} instance that will configure any
     * provided {@link Handle} instances.
     * @param plugin the plugin to install
     * @return this
     */
    public Jdbi installPlugin(JdbiPlugin plugin) {
        Unchecked.consumer(plugin::customizeJdbi).accept(this);
        plugins.add(plugin);
        return this;
    }

    /**
     * Allows customization of how prepared statements are created. When a Handle is created
     * against this Jdbi instance the factory will be used to create a StatementBuilder for
     * that specific handle. When the handle is closed, the StatementBuilder's close method
     * will be invoked.
     *
     * @param factory the new statement builder factory.
     * @return this
     */
    public Jdbi setStatementBuilderFactory(StatementBuilderFactory factory) {
        this.statementBuilderFactory.set(factory);
        return this;
    }

    /**
     * @return the current {@link StatementBuilderFactory}
     */
    public StatementBuilderFactory getStatementBuilderFactory() {
        return this.statementBuilderFactory.get();
    }

    @Override
    public ConfigRegistry getConfig() {
        return config;
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
     *                from this Jdbi
     * @return this
     */
    public Jdbi setTransactionHandler(TransactionHandler handler) {
        Objects.requireNonNull(handler, "null transaction handler");
        this.transactionhandler.set(handler);
        return this;
    }

    /**
     * @return the {@link TransactionHandler}
     */
    public TransactionHandler getTransactionHandler() {
        return this.transactionhandler.get();
    }

    /**
     * Obtain a Handle to the data source wrapped by this Jdbi instance.
     * You own this expensive resource and are required to close it or
     * risk leaks.  Using a {@code try-with-resources} block is recommended.
     *
     * @return an open Handle instance
     * @see #useHandle(HandleConsumer)
     * @see #withHandle(HandleCallback)
     */
    public Handle open() {
        try {
            final long start = System.nanoTime();
            @SuppressWarnings("PMD.CloseResource")
            Connection conn = connectionFactory.openConnection();
            final long stop = System.nanoTime();

            for (JdbiPlugin p : plugins) {
                conn = p.customizeConnection(conn);
            }

            StatementBuilder cache = statementBuilderFactory.get().createStatementBuilder(conn);
            Handle h = new Handle(config.createCopy(), connectionFactory::closeConnection, transactionhandler.get(), cache, conn);
            for (JdbiPlugin p : plugins) {
                h = p.customizeHandle(h);
            }
            LOG.trace("Jdbi [{}] obtain handle [{}] in {}ms", this, h, MILLISECONDS.convert(stop - start, NANOSECONDS));
            return h;
        } catch (SQLException e) {
            throw new ConnectionException(e);
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
    public <R, X extends Exception> R withHandle(HandleCallback<R, X> callback) throws X {
        if (threadHandle.get() != null) {
            return callback.withHandle(threadHandle.get());
        }

        try (Handle h = this.open()) {
            threadHandle.set(h);
            return callback.withHandle(h);
        } finally {
            threadHandle.remove();
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
    public <X extends Exception> void useHandle(final HandleConsumer<X> callback) throws X {
        withHandle(h -> {
            callback.useHandle(h);
            return null;
        });
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
    public <R, X extends Exception> R inTransaction(final HandleCallback<R, X> callback) throws X {
        return withHandle(handle -> handle.<R, X>inTransaction(callback));
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useTransaction(final HandleConsumer<X> callback) throws X {
        useHandle(handle -> handle.useTransaction(callback));
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     *
     * @param level the transaction isolation level which will be applied to the connection for the scope of this
     *              transaction, after which the original isolation level will be restored.
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return the value returned by callback
     *
     * @throws X any exception thrown by the callback
     */
    public <R, X extends Exception> R inTransaction(final TransactionIsolationLevel level, final HandleCallback<R, X> callback) throws X {
        return withHandle(handle -> handle.inTransaction(level, callback));
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients. The handle will be in a transaction when the callback is invoked, and
     * that transaction will be committed if the callback finishes normally, or rolled back if the
     * callback raises an exception.
     *
     * <p>
     * This form accepts a transaction isolation level which will be applied to the connection
     * for the scope of this transaction, after which the original isolation level will be restored.
     * </p>
     *
     * @param level the transaction isolation level which will be applied to the connection for the scope of this
     *              transaction, after which the original isolation level will be restored.
     * @param callback A callback which will receive an open Handle, in a transaction
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useTransaction(final TransactionIsolationLevel level, final HandleConsumer<X> callback) throws X {
        useHandle(handle -> handle.useTransaction(level, callback));
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
            throws NoSuchExtensionException, X {
        if (threadHandle.get() != null) {
            return callback.withExtension(threadHandle.get().attach(extensionType));
        }

        try (LazyHandleSupplier handle = new LazyHandleSupplier(this, config)) {
            E extension = getConfig(Extensions.class)
                    .findFor(extensionType, handle)
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
        if (!getConfig(Extensions.class).hasExtensionFor(extensionType)) {
            throw new NoSuchExtensionException("Extension not found: " + extensionType);
        }

        return OnDemandExtensions.create(this, extensionType);
    }
}

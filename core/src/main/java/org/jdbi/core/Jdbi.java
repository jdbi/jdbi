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
package org.jdbi.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.ConfigView;
import org.jdbi.core.config.Configurable;
import org.jdbi.core.extension.ExtensionCallback;
import org.jdbi.core.extension.ExtensionConsumer;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.Extensions;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.extension.NoSuchExtensionException;
import org.jdbi.core.internal.OnDemandExtensions;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.ConfigReader;
import org.jdbi.core.statement.DefaultStatementBuilder;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.StatementBuilder;
import org.jdbi.core.statement.StatementBuilderFactory;
import org.jdbi.core.statement.StatementTemplate;
import org.jdbi.core.transaction.LocalTransactionHandler;
import org.jdbi.core.transaction.TransactionHandler;
import org.jdbi.core.transaction.TransactionIsolationLevel;
import org.jdbi.meta.Alpha;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Main entry point; configurable wrapper around a JDBC {@link DataSource}.
 * Use it to obtain Handle instances and provide configuration
 * for all handles obtained from it.
 */
public class Jdbi implements ConfigReader {
    private static final Logger LOG = LoggerFactory.getLogger(Jdbi.class);

    /** A no-op per-handle config scope: the opened handle uses an unmodified copy of this Jdbi's config. */
    private static final Consumer<ConfigRegistry> NO_CONFIG_SCOPE = config -> {};

    private final ConfigRegistry config;
    // Read-only view handed to callers via getConfig(): a distinct delegate that cannot be cast back to the mutable
    // ConfigRegistry, so post-build configuration must go through the builder. Internal code uses `config` directly.
    private final ConfigView configView = ConfigView.readOnly(this::configRegistry);

    private final ConnectionFactory connectionFactory;
    private final AtomicReference<TransactionHandler> transactionhandler = new AtomicReference<>(LocalTransactionHandler.binding());
    private final AtomicReference<StatementBuilderFactory> statementBuilderFactory = new AtomicReference<>(DefaultStatementBuilder.FACTORY);
    private final AtomicReference<HandleCallbackDecorator> handleCallbackDecorator = new AtomicReference<>(HandleCallbackDecorator.STANDARD_HANDLE_CALLBACK_DECORATOR);
    private HandleScope handleScope = HandleScope.threadLocal();

    private final CopyOnWriteArrayList<JdbiPlugin> plugins = new CopyOnWriteArrayList<>();

    private Jdbi(final ConnectionFactory connectionFactory) {
        this(connectionFactory, new ConfigRegistry());
    }

    private Jdbi(final ConnectionFactory connectionFactory, final ConfigRegistry config) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "null connectionFactory");
        this.config = config;
    }

    /**
     * Creates a new {@link Jdbi} object from a {@link Connection}.
     *
     * @param connection A {@link Connection} object.
     *
     * @return A {@link Jdbi} instance that uses a single database connection.
     */
    public static Jdbi create(final Connection connection) {
        return create(new SingleConnectionFactory(connection));
    }

    /**
     * Creates a new {@link Jdbi} object from a {@link DataSource}.
     *
     * @param dataSource the data source.
     *
     * @return a Jdbi which uses the given data source as a connection factory.
     */
    public static Jdbi create(final DataSource dataSource) {
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
    public static Jdbi create(final ConnectionFactory connectionFactory) {
        return new Jdbi(connectionFactory);
    }

    /**
     * Creates a new {@link Jdbi} instance from a database URL.
     *
     * @param url A JDBC URL for connections.
     *
     * @return a Jdbi which uses{@link DriverManager} as a connection factory.
     */
    public static Jdbi create(final String url) {
        Objects.requireNonNull(url, "null url");
        return create(() -> DriverManager.getConnection(url));
    }

    /**
     * Creates a new {@link Jdbi} instance from a database URL.
     *
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
     * Creates a new {@link Jdbi} instance from a database URL.
     *
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
     * Returns a {@link Builder} that assembles a {@link Jdbi} against the given connection factory. The builder is
     * the preferred way to configure a {@code Jdbi}: register mappers, arguments, and plugins, set the transaction
     * handler and other knobs, then call {@link Builder#build()}.
     *
     * @param connectionFactory provides JDBC connections to {@link Handle} instances
     * @return a builder for a {@code Jdbi} using the given connection factory
     */
    @Alpha
    public static Builder builder(final ConnectionFactory connectionFactory) {
        return new Builder(create(connectionFactory));
    }

    /**
     * Returns a {@link Builder} that assembles a {@link Jdbi} against the given data source.
     *
     * @param dataSource the data source.
     * @return a builder for a {@code Jdbi} using the given data source
     * @see #builder(ConnectionFactory)
     */
    @Alpha
    public static Builder builder(final DataSource dataSource) {
        return new Builder(create(dataSource));
    }

    /**
     * Returns a {@link Builder} that assembles a {@link Jdbi} against a single database connection.
     *
     * @param connection a {@link Connection} object.
     * @return a builder for a {@code Jdbi} using the given connection
     * @see #builder(ConnectionFactory)
     */
    @Alpha
    public static Builder builder(final Connection connection) {
        return new Builder(create(connection));
    }

    /**
     * Returns a {@link Builder} that assembles a {@link Jdbi} against connections obtained from the given URL.
     *
     * @param url a JDBC URL for connections.
     * @return a builder for a {@code Jdbi} using the given URL
     * @see #builder(ConnectionFactory)
     */
    @Alpha
    public static Builder builder(final String url) {
        return new Builder(create(url));
    }

    /**
     * Returns a {@link Builder} that assembles a {@link Jdbi} against connections obtained from the given URL and properties.
     *
     * @param url        a JDBC URL for connections.
     * @param properties properties passed to {@link DriverManager#getConnection(String, Properties)} for each handle.
     * @return a builder for a {@code Jdbi} using the given URL and properties
     * @see #builder(ConnectionFactory)
     */
    @Alpha
    public static Builder builder(final String url, final Properties properties) {
        return new Builder(create(url, properties));
    }

    /**
     * Returns a {@link Builder} that assembles a {@link Jdbi} against connections obtained from the given URL, user, and password.
     *
     * @param url      a JDBC URL for connections.
     * @param username the username for the connection.
     * @param password the password for the connection.
     * @return a builder for a {@code Jdbi} using the given URL and credentials
     * @see #builder(ConnectionFactory)
     */
    @Alpha
    public static Builder builder(final String url, final String username, final String password) {
        return new Builder(create(url, username, password));
    }

    /**
     * Convenience method used to obtain a handle from a specific data source
     *
     * @param dataSource the JDBC data source.
     *
     * @return Handle using a Connection obtained from the provided DataSource
     */
    public static Handle open(final DataSource dataSource) {
        return create(dataSource).open();
    }

    /**
     * Convenience method used to obtain a handle from a {@link ConnectionFactory}.
     *
     * @param connectionFactory the connection factory
     *
     * @return Handle using a Connection obtained from the provided connection factory
     */
    public static Handle open(final ConnectionFactory connectionFactory) {
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
     * Applies a single plugin to this instance during assembly: it is registered once for the per-handle and
     * per-connection hooks, then its {@link JdbiPlugin#configure(Builder)} hook runs. The install-if-absent guard
     * makes a plugin pulled in more than once apply only once. Shared by {@link Builder#build()}.
     */
    private void applyPlugin(final Builder builder, final JdbiPlugin plugin) {
        if (plugins.addIfAbsent(plugin)) {
            plugin.configure(builder);
        }
    }

    /**
     * Returns the current {@link StatementBuilderFactory}.
     *
     * @return the current {@link StatementBuilderFactory}
     */
    public StatementBuilderFactory getStatementBuilderFactory() {
        return this.statementBuilderFactory.get();
    }

    @Override
    public ConfigView getConfig() {
        return configView;
    }

    /**
     * The mutable registry backing {@link #getConfig()}. Package-private, for framework code (the extension
     * machinery, handle suppliers) that must reach the live registry; the public surface is read-only.
     */
    ConfigRegistry configRegistry() {
        return config;
    }

    /**
     * Returns a {@link Builder} seeded from this Jdbi: a copy of this instance's configuration, and the same
     * connection source, transaction handler, statement-builder factory, handle callback decorator, handle scope,
     * and installed plugins. Reconfigure it and call {@link Builder#build()} to obtain an independent {@code Jdbi}
     * that shares this instance's connection source but has its own configuration &mdash; useful for deriving a
     * long-lived variant (for example, per-tenant) without disturbing this instance:
     * <pre>{@code
     * Jdbi tenantJdbi = jdbi.toBuilder()
     *     .configure(SqlStatements.class, s -> s.setSqlLogger(tenantLogger))
     *     .build();
     * }</pre>
     * The seeded plugins are already applied &mdash; their configuration is baked into the copied config and their
     * per-handle hooks run on handles from the built instance &mdash; so {@link Builder#build()} does not re-run
     * their {@link JdbiPlugin#configure(Builder)} hook. To vary configuration for a single unit of work rather than
     * a long-lived instance, prefer {@link #open(java.util.function.Consumer)} or per-statement configuration.
     *
     * @return a builder seeded from this Jdbi's configuration and connection source
     */
    @Alpha
    public Builder toBuilder() {
        final Jdbi derived = new Jdbi(connectionFactory, config.createCopy());
        derived.transactionhandler.set(transactionhandler.get());
        derived.statementBuilderFactory.set(statementBuilderFactory.get());
        derived.handleCallbackDecorator.set(handleCallbackDecorator.get());
        derived.handleScope = handleScope;
        derived.plugins.addAll(plugins);
        return new Builder(derived);
    }

    /**
     * Returns the {@link TransactionHandler}.
     *
     * @return the {@link TransactionHandler}
     */
    public TransactionHandler getTransactionHandler() {
        return this.transactionhandler.get();
    }

    /**
     * Returns the {@link HandleCallbackDecorator}.
     *
     * @return the {@link HandleCallbackDecorator}
     */
    @Alpha
    public HandleCallbackDecorator getHandleCallbackDecorator() {
        return this.handleCallbackDecorator.get();
    }

    /**
     * Returns the internal {@link HandleScope} object. The Jdbi instance uses this to provide handles in a given scope.
     * The default scope is <i>per-thread</i>, so every thread manages its own handle.
     * <br>
     * <b>This is an internal method and not part of the public API!</b>
     * @return A {@link HandleScope} object
     */
    public final HandleScope getHandleScope() {
        return handleScope;
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
        return open(NO_CONFIG_SCOPE);
    }

    /**
     * Obtain a Handle whose configuration is a copy of this Jdbi's, with the given scope applied. Use this to run a
     * unit of work against a handle that carries per-handle configuration (mappers, arguments, defines, and the like)
     * without affecting this Jdbi or any other handle:
     * <pre>{@code
     * try (Handle h = jdbi.open(config -> config.configure(RowMappers.class, r -> r.register(myMapper)))) {
     *     ...
     * }
     * }</pre>
     * The scope receives a private copy of this Jdbi's config and configures it in place. You own the returned handle
     * and are required to close it, ideally with a {@code try-with-resources} block.
     *
     * @param configScope applied to the new handle's private config copy during {@code open}
     * @return an open Handle instance carrying the scoped configuration
     * @see #open()
     * @see #withHandle(Consumer, HandleCallback)
     */
    @Alpha
    public Handle open(final Consumer<ConfigRegistry> configScope) {
        Objects.requireNonNull(configScope, "null configScope");
        try {
            final long start = System.nanoTime();
            Connection conn = Objects.requireNonNull(connectionFactory.openConnection(),
                    () -> "Connection factory " + connectionFactory + " returned a null connection");
            final long stop = System.nanoTime();

            try {
                for (final JdbiPlugin p : plugins) {
                    conn = p.customizeConnection(conn);
                }

                final StatementBuilder cache = statementBuilderFactory.get().createStatementBuilder(conn);

                Handle h = Handle.createHandle(this,
                        connectionFactory.getCleanableFor(conn), // don't use conn::close, the cleanup must be done by the connection factory!
                        transactionhandler.get(),
                        cache,
                        conn,
                        configScope);

                for (final JdbiPlugin p : plugins) {
                    h = p.customizeHandle(h);
                }
                LOG.trace("Jdbi [{}] obtain handle [{}] in {}ms", this, h, MILLISECONDS.convert(stop - start, NANOSECONDS));
                return h;
            } catch (final Throwable t) {
                connectionFactory.getCleanableFor(conn).closeAndSuppress(t);
                throw t;
            }
        } catch (final SQLException e) {
            throw new ConnectionException(e);
        }
    }

    /**
     * Applies each installed plugin's {@link JdbiPlugin#customizeHandleConfig} to a new handle's config during
     * construction, in install order. Called from the {@link Handle} constructor.
     */
    void customizeHandleConfig(final Connection connection, final ConfigRegistry config) throws SQLException {
        for (final JdbiPlugin p : plugins) {
            p.customizeHandleConfig(connection, config);
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
    public <R, X extends Exception> R withHandle(final HandleCallback<R, X> callback) throws X {
        final HandleCallback<R, X> decoratedCallback = handleCallbackDecorator.get().decorate(callback);

        final var handleSupplier = handleScope.get();
        if (handleSupplier != null) {
            return decoratedCallback.withHandle(handleSupplier.getHandle());
        }

        try (Handle h = this.open()) {
            h.setForceAttachStatements(h.getConfig().get(SqlStatements.class).isAttachCallbackStatementsForCleanup());

            handleScope.set(ConstantHandleSupplier.of(h));
            return decoratedCallback.withHandle(h);
        } finally {
            handleScope.clear();
        }
    }

    /**
     * A convenience function which manages the lifecycle of a handle carrying the given per-handle configuration,
     * and yields it to a callback. Unlike {@link #withHandle(HandleCallback)}, this always opens a <em>new</em> handle
     * for the scoped configuration (a handle captures its config at {@code open}), so it does not join a handle already
     * in scope; the scoped handle is in scope for the duration of the callback and is closed before returning.
     *
     * @param configScope applied to the new handle's private config copy during {@code open}
     * @param callback A callback which will receive the scoped Handle
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return the value returned by callback
     *
     * @throws X any exception thrown by the callback
     * @see #open(Consumer)
     */
    @Alpha
    public <R, X extends Exception> R withHandle(final Consumer<ConfigRegistry> configScope, final HandleCallback<R, X> callback) throws X {
        Objects.requireNonNull(configScope, "null configScope");
        final HandleCallback<R, X> decoratedCallback = handleCallbackDecorator.get().decorate(callback);

        final var previous = handleScope.get();
        try (Handle h = this.open(configScope)) {
            h.setForceAttachStatements(h.getConfig().get(SqlStatements.class).isAttachCallbackStatementsForCleanup());

            handleScope.set(ConstantHandleSupplier.of(h));
            return decoratedCallback.withHandle(h);
        } finally {
            if (previous == null) {
                handleScope.clear();
            } else {
                handleScope.set(previous);
            }
        }
    }

    /**
     * A convenience function which manages the lifecycle of a handle and yields it to a callback
     * for use by clients.
     *
     * @param consumer A callback which will receive an open Handle
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    public <X extends Exception> void useHandle(final HandleConsumer<X> consumer) throws X {
        withHandle(consumer.asCallback());
    }

    /**
     * A convenience function which manages the lifecycle of a handle carrying the given per-handle configuration,
     * and yields it to a callback. Like {@link #withHandle(Consumer, HandleCallback)}, this always opens a new handle
     * for the scoped configuration.
     *
     * @param configScope applied to the new handle's private config copy during {@code open}
     * @param consumer A callback which will receive the scoped Handle
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    @Alpha
    public <X extends Exception> void useHandle(final Consumer<ConfigRegistry> configScope, final HandleConsumer<X> consumer) throws X {
        withHandle(configScope, consumer.asCallback());
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
        return withHandle(handle -> handle.inTransaction(callback));
    }

    /**
     * A convenience function which manages the lifecycle of a handle carrying the given per-handle configuration,
     * and yields it to a callback in a transaction. The transaction is committed if the callback finishes normally,
     * or rolled back if it raises an exception. Like {@link #withHandle(Consumer, HandleCallback)}, this always opens
     * a new handle for the scoped configuration.
     *
     * @param configScope applied to the new handle's private config copy during {@code open}
     * @param callback A callback which will receive the scoped Handle, in a transaction
     * @param <R> type returned by the callback
     * @param <X> exception type thrown by the callback, if any.
     *
     * @return the value returned by callback
     *
     * @throws X any exception thrown by the callback
     */
    @Alpha
    public <R, X extends Exception> R inTransaction(final Consumer<ConfigRegistry> configScope, final HandleCallback<R, X> callback) throws X {
        return withHandle(configScope, handle -> handle.inTransaction(callback));
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
     * A convenience function which manages the lifecycle of a handle carrying the given per-handle configuration,
     * and yields it to a callback in a transaction. The transaction is committed if the callback finishes normally,
     * or rolled back if it raises an exception. Like {@link #withHandle(Consumer, HandleCallback)}, this always opens
     * a new handle for the scoped configuration.
     *
     * @param configScope applied to the new handle's private config copy during {@code open}
     * @param callback A callback which will receive the scoped Handle, in a transaction
     * @param <X> exception type thrown by the callback, if any.
     *
     * @throws X any exception thrown by the callback
     */
    @Alpha
    public <X extends Exception> void useTransaction(final Consumer<ConfigRegistry> configScope, final HandleConsumer<X> callback) throws X {
        useHandle(configScope, handle -> handle.useTransaction(callback));
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
    public <R, E, X extends Exception> R withExtension(final Class<E> extensionType, final ExtensionCallback<R, E, X> callback)
            throws X {
        final var handleSupplier = handleScope.get();
        if (handleSupplier != null) {
            return callWithExtension(extensionType, callback, handleSupplier);
        }

        try (LazyHandleSupplier lazyHandleSupplier = new LazyHandleSupplier(this)) {
            handleScope.set(lazyHandleSupplier);
            return callWithExtension(extensionType, callback, lazyHandleSupplier);
        } finally {
            handleScope.clear();
        }
    }

    private <R, E, X extends Exception> R callWithExtension(final Class<E> extensionType,
                                                            final ExtensionCallback<R, E, X> callback,
                                                            final HandleSupplier handleSupplier) throws X {
        final E extension = getConfig(Extensions.class)
            .findFor(extensionType, handleSupplier)
            .orElseThrow(() -> new NoSuchExtensionException(extensionType));

        return callback.withExtension(extension);
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
    public <E, X extends Exception> void useExtension(final Class<E> extensionType, final ExtensionConsumer<E, X> callback)
            throws X {
        withExtension(extensionType, extension -> {
            callback.useExtension(extension);
            return null;
        });
    }

    /**
     * Creates an extension instance that uses the current {@link Jdbi} instance for database operations.
     *
     * @param extensionType the type of extension. Must be a public interface type.
     * @param <E> the extension type
     *
     * @return an extension which opens and closes handles (as needed) for individual method calls. Only public
     * interface types may be used as on-demand extensions.
     */
    public <E> E onDemand(final Class<E> extensionType) {
        if (!extensionType.isInterface()) {
            throw new IllegalArgumentException("On-demand extensions are only supported for interfaces.");
        }
        if (!getConfig(Extensions.class).hasExtensionFor(extensionType)) {
            throw new NoSuchExtensionException(extensionType);
        }

        return getConfig(OnDemandExtensions.class).create(this, extensionType);
    }

    /**
     * Builds a reusable, thread-safe {@link StatementTemplate} over the given SQL, prepared once against a
     * snapshot of this Jdbi's configuration and then executed many times against any handle via
     * {@link StatementTemplate#with(Handle)}. Reusing a template is cheaper than building an equivalent
     * statement on each call.
     *
     * @param sql the SQL for the template
     * @return a reusable statement template
     */
    public StatementTemplate buildStatementTemplate(final CharSequence sql) {
        return new StatementTemplate(config.createChild(), sql);
    }

    /**
     * Assembles a {@link Jdbi} instance. Obtain one from {@link Jdbi#builder(ConnectionFactory)} (or an overload),
     * register mappers, arguments, and plugins, set the transaction handler and other knobs, then call
     * {@link #build()}. The builder is {@link Configurable}, so all of the {@code register*}/{@code configure}
     * convenience methods are available during assembly.
     * <p>
     * A builder is single-use: {@link #build()} returns the assembled {@code Jdbi} and should be called once.
     */
    @Alpha
    public static final class Builder implements Configurable<Builder> {

        private final Jdbi jdbi;
        private final List<JdbiPlugin> plugins = new ArrayList<>();

        Builder(final Jdbi jdbi) {
            this.jdbi = jdbi;
        }

        @Override
        public ConfigRegistry getConfig() {
            // The builder configures the Jdbi's registry in place during assembly, so it returns the mutable
            // registry directly rather than the read-only view exposed by Jdbi.getConfig().
            return jdbi.config;
        }

        /**
         * Installs a {@link JdbiPlugin} to be applied when {@link #build()} is called. Plugins are applied in the
         * order installed.
         *
         * @param plugin the plugin to install
         * @return this builder
         */
        public Builder installPlugin(final JdbiPlugin plugin) {
            Objects.requireNonNull(plugin, "null plugin");
            if (!plugins.contains(plugin)) {
                plugins.add(plugin);
            }
            return this;
        }

        /**
         * Sets the {@link TransactionHandler} for the assembled {@code Jdbi}.
         *
         * @param handler the transaction handler
         * @return this builder
         */
        public Builder transactionHandler(final TransactionHandler handler) {
            jdbi.transactionhandler.set(Objects.requireNonNull(handler, "null transaction handler"));
            return this;
        }

        /**
         * Sets the {@link StatementBuilderFactory} for the assembled {@code Jdbi}.
         *
         * @param factory the statement builder factory
         * @return this builder
         */
        public Builder statementBuilderFactory(final StatementBuilderFactory factory) {
            jdbi.statementBuilderFactory.set(factory);
            return this;
        }

        /**
         * Sets the {@link HandleCallbackDecorator} for the assembled {@code Jdbi}.
         *
         * @param handleCallbackDecorator the handle callback decorator
         * @return this builder
         */
        public Builder handleCallbackDecorator(final HandleCallbackDecorator handleCallbackDecorator) {
            jdbi.handleCallbackDecorator.set(Objects.requireNonNull(handleCallbackDecorator, "null handler"));
            return this;
        }

        /**
         * Sets the {@link HandleScope} for the assembled {@code Jdbi}.
         *
         * @param handleScope the handle scope
         * @return this builder
         */
        public Builder handleScope(final HandleScope handleScope) {
            jdbi.handleScope = handleScope;
            return this;
        }

        /**
         * Applies the installed plugins and returns the assembled {@link Jdbi}. Each plugin's
         * {@link JdbiPlugin#configure(Builder)} runs, in install order. A plugin may itself install further plugins
         * (via {@link #installPlugin(JdbiPlugin)} from its {@code configure} hook); those are drained and applied in
         * turn, each at most once.
         *
         * @return the assembled {@code Jdbi}
         */
        public Jdbi build() {
            // Drain by index: a plugin's configure() may install further plugins, growing the list mid-drain.
            // applyPlugin() installs-if-absent so a plugin pulled in by two others is still applied once.
            int i = 0;
            while (i < plugins.size()) {
                jdbi.applyPlugin(this, plugins.get(i++));
            }
            return jdbi;
        }
    }
}

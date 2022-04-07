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
package org.jdbi.v3.testing.junit5;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Common functionality for all JUnit 5 extensions.
 * <p>
 * Subclasses can be used with the {@code @ExtendWith} annotation to declare an extension if they provide a public no-args constructor.
 * <p>
 * When using declarative registration, test methods can declare a {@link Jdbi} and/or a {@link Handle} parameter which is injected through this extension. The
 * {@link #getJdbi()} is used to obtain the {@link Jdbi} object and {@link #getSharedHandle()}} is used for the {@link Handle} instance.
 * <p>
 * Programmatic registration is preferred as this allows further customization of each extension.
 *
 * @see JdbiH2Extension
 * @see JdbiPostgresExtension
 * @see JdbiSqliteExtension
 * @see JdbiExternalPostgresExtension
 * @see JdbiOtjPostgresExtension
 */
public abstract class JdbiExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    private final Set<JdbiPlugin> plugins = new LinkedHashSet<>();
    private Optional<JdbiExtensionInitializer> initializerMaybe = Optional.empty();

    private boolean installPlugins = false;

    private volatile Jdbi jdbi;
    private volatile Handle sharedHandle;

    private volatile boolean dataSourceInitialized = false;
    private volatile DataSource dataSource;

    /**
     * Creates a new extension using a managed, embedded postgres database. Using this method requires the <code>de.softwareforge.testing:pg-embedded</code>
     * component and the postgres JDBC driver on the class path.
     * <p>
     * It references an embedded pg extension, which must be added to the class independently and can be managed as a per-class extension, not a per-method
     * extension:
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();
     *
     *     @RegisterExtension
     *     public JdbiExtension postgres = JdbiExtension.postgres(pg);
     * }</pre>
     * <p>
     * This is the most efficient way to run multiple tests within the same class that use a postgres database.
     *
     * @param pg Reference to an embedded postgres database. This extension must be separately added to the test class.
     * @return A {@link JdbiExtension} connected to a managed postgres data source.
     * @see JdbiPostgresExtension
     */
    public static JdbiExtension postgres(EmbeddedPgExtension pg) {
        return JdbiPostgresExtension.instance(pg);
    }

    /**
     * Creates an extension that uses an external (outside the scope of an unit test class) postgres database. Using this method requires the postgres JDBC
     * driver on the classpath.
     *
     * @param hostname Hostname of the database.
     * @param port     Port of the database. Can be null, in that case the default port is used.
     * @param database Database name.
     * @param username Username to access the database. Can be null.
     * @param password Password to access the database. Can be null.
     * @return A {@link JdbiExtension} connected to an external postgres data source.
     * @see JdbiExternalPostgresExtension
     */
    @SuppressWarnings("PMD.UseObjectForClearerAPI")
    public static JdbiExtension externalPostgres(@Nonnull String hostname,
        @Nullable Integer port,
        @Nonnull String database,
        @Nullable String username,
        @Nullable String password) {
        return JdbiExternalPostgresExtension.instance(hostname, port, database, username, password);
    }

    /**
     * Creates a new extension using a managed, embedded postgres database. Using this method requires the <code>com.opentable.components:otj-pg-embedded</code>
     * component and the postgres JDBC driver on the class path.
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public JdbiExtension postgres = JdbiExtension.otjEmbeddedPostgres();
     * }</pre>
     * <p>
     * Compared to the {@link #postgres(EmbeddedPgExtension)} method, this extension spins up a new postgres instance for each test method and is slower. It
     * should only be used to migrate existing code that uses the JUnit 4 {@link org.jdbi.v3.testing.JdbiRule} quickly to JUnit 5.
     *
     * @return A {@link JdbiExtension} connected to a managed postgres data source.
     * @see JdbiOtjPostgresExtension
     */
    public static JdbiOtjPostgresExtension otjEmbeddedPostgres() {
        return JdbiOtjPostgresExtension.instance();
    }

    /**
     * Creates a new extension using the H2 database. Each call to this method creates a new database which is transient and in-memory. Using this method
     * requires the <code>com.h2database:h2</code> component on the classpath.
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public JdbiExtension h2 = JdbiExtension.h2();
     * }</pre>
     *
     * @return A {@link JdbiExtension} connected to a managed h2 data source.
     * @see JdbiH2Extension
     */
    public static JdbiExtension h2() {
        return JdbiH2Extension.instance();
    }

    /**
     * Creates a new extension using the SQLite database. Each call to this method creates a new database which is transient and in-memory. Using this method
     * requires the <code>org.xerial:sqlite-jdbc</code> component on the classpath.
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public JdbiExtension sqlite = JdbiExtension.sqlite();
     * }</pre>
     *
     * @return A {@link JdbiExtension} connected to a managed sqlite data source.
     * @see JdbiSqliteExtension
     */
    public static JdbiExtension sqlite() {
        return JdbiSqliteExtension.instance();
    }

    protected JdbiExtension() {}

    /**
     * Returns a {@link Jdbi} instance linked to the data source used by this extension. There is only a single Jdbi instance and multiple calls to this method
     * return the same instance.
     *
     * @return A {@link Jdbi} instance.
     */
    public final Jdbi getJdbi() {
        if (jdbi == null) {
            throw new IllegalStateException("jdbi is null!");
        }

        return jdbi;
    }

    /**
     * Returns a JDBC url representing the data source used by this extension. This url is database-specific and may or may not be used to connect to the data
     * source outside testing code that uses this extension (e.g. the {@link JdbiSqliteExtension} returns a constant uri for all database instances).
     *
     * @return A string representing the JDBC URL.
     */
    public abstract String getUrl();

    /**
     * Returns a shared {@link Handle} used by the extension. This handle exists during the lifetime of the extension and is connected to the data source used
     * by the extension. Multiple calls to this method always return the same instance.
     *
     * @return A {@link Handle} instance.
     */
    public final Handle getSharedHandle() {
        if (sharedHandle == null) {
            throw new IllegalStateException("sharedHandle is null!");
        }

        return sharedHandle;
    }

    /**
     * When creating the {@link Jdbi} instance, call the {@link Jdbi#installPlugins()} method, which loads all plugins discovered by the {@link
     * java.util.ServiceLoader} API.
     *
     * @return The extension itself for chaining method calls.
     */
    public final JdbiExtension installPlugins() {
        this.installPlugins = true;

        return this;
    }

    /**
     * Install a {@link JdbiPlugin} when creating the {@link Jdbi} instance.
     *
     * @param plugin A plugin to install.
     * @return The extension itself for chaining method calls.
     */
    public final JdbiExtension withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);

        return this;
    }

    /**
     * Install multiple {@link JdbiPlugin}s when creating the {@link Jdbi} instance.
     *
     * @param pluginList One or more {@link JdbiPlugin} instances.
     * @return The extension itself for chaining method calls.
     */
    public final JdbiExtension withPlugins(JdbiPlugin... pluginList) {
        this.plugins.addAll(Arrays.asList(pluginList));

        return this;
    }

    /**
     * Sets a {@link JdbiExtensionInitializer} to initialize the {@link Jdbi} instance or the attached data source before running a test.
     *
     * @return The extension itself for chaining method calls.
     */
    public final JdbiExtension withInitializer(JdbiExtensionInitializer initializer) {
        if (initializer == null) {
            throw new IllegalStateException("initializer is null!");
        }

        if (initializerMaybe.isPresent()) {
            throw new IllegalStateException("initializer already defined!");
        }

        this.initializerMaybe = Optional.of(initializer);

        return this;
    }

    /**
     * Open a new {@link Handle} to the used data source. Multiple calls to this method return multiple instances that are all connected to the same data
     * source.
     *
     * @return A {@link Handle} object. The handle must be closed to release the database connection.
     */
    public final Handle openHandle() {
        return getJdbi().open();
    }

    /**
     * Set a {@link JdbiConfig} parameter when creating the {@link Jdbi} instance. Any {@link JdbiConfig} type can be referenced in this method call.
     *
     * <pre>{@code
     *     @RegisterExtension
     *     public JdbiExtension h2Extension = JdbiExtension.h2().withPlugin(new SqlObjectPlugin())
     *             .withConfig(RowMappers.class, r -> r.register(Foo.class, new FooMapper());
     * }</pre>
     *
     * @param configClass A class instance which must implement {@link JdbiConfig}.
     * @param configurer  A {@link Consumer} to access the {@link JdbiConfig} instance.
     * @param <C>         The config type. Must extend {@link JdbiConfig}.
     * @return The extension itself for chaining method calls.
     */
    public final <C extends JdbiConfig<C>> JdbiExtension withConfig(Class<C> configClass, Consumer<C> configurer) {
        return withPlugin(ConfiguringPlugin.of(configClass, configurer));
    }

    /**
     * Convenience method to attach an extension (such as a SqlObject) to the shared handle.
     */
    public final <T> T attach(final Class<T> extension) {
        return getSharedHandle().attach(extension);
    }

    protected abstract DataSource createDataSource() throws Exception;

    private DataSource getDataSource() throws Exception {
        // Taken from Guava Suppliers.memoize()
        if (!dataSourceInitialized) {
            synchronized (this) {
                if (!dataSourceInitialized) {
                    DataSource ds = createDataSource();
                    dataSource = ds;
                    dataSourceInitialized = true;
                    return ds;
                }
            }
        }
        return dataSource;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (jdbi != null) {
            throw new IllegalStateException("jdbi has been set!");
        }

        final DataSource ds = getDataSource();

        final Jdbi jdbiInstance = Jdbi.create(ds);

        if (installPlugins) {
            jdbiInstance.installPlugins();
        }

        plugins.forEach(jdbiInstance::installPlugin);
        final Handle sharedHandleInstance = jdbiInstance.open();

        this.jdbi = jdbiInstance;
        this.sharedHandle = sharedHandleInstance;

        initializerMaybe.ifPresent(i -> i.initialize(ds, sharedHandleInstance));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (sharedHandle == null) {
            throw new IllegalStateException("shared handle was not initialized!");
        }

        try {
            final DataSource ds = getDataSource();
            Handle handle = sharedHandle;

            initializerMaybe.ifPresent(i -> i.cleanup(ds, sharedHandle));

            handle.close();
        } finally {
            this.dataSourceInitialized = false;

            this.dataSource = null;
            this.sharedHandle = null;
            this.jdbi = null;
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Type type = parameterContext.getParameter().getType();
        return type == Jdbi.class || type == Handle.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Type type = parameterContext.getParameter().getType();
        if (type == Jdbi.class) {
            return getJdbi();
        } else if (type == Handle.class) {
            return getSharedHandle();
        }
        return null;
    }

    private static final class ConfiguringPlugin<C extends JdbiConfig<C>> implements JdbiPlugin {

        private final Class<C> configClass;
        private final Consumer<C> configurer;

        private ConfiguringPlugin(Class<C> configClass, Consumer<C> configurer) {
            this.configClass = configClass;
            this.configurer = configurer;
        }

        static <C extends JdbiConfig<C>> ConfiguringPlugin<C> of(Class<C> configClass, Consumer<C> configurer) {
            return new ConfiguringPlugin<>(configClass, configurer);
        }

        @Override
        public void customizeJdbi(Jdbi jdbi) {
            jdbi.configure(configClass, configurer);
        }
    }
}

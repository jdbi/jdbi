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
package org.jdbi.v3.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcConnectionPool;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;

/**
 * JUnit {@code @Rule} to manage a Jdbi instance pointed to a managed database.
 */
public abstract class JdbiRule extends ExternalResource {

    private volatile DataSource dataSource;
    private Jdbi jdbi;
    private Handle handle;
    private boolean installPlugins;
    private String[] migrationScriptPaths;
    private List<JdbiPlugin> plugins = new ArrayList<>();

    private Object mutex = new Object();

    public JdbiRule() {
        if (!(isOverridden("createJdbi") || isOverridden("createDataSource"))) {
            throw new IllegalStateException("JdbiRule implementations must override at least one of createDataSource() (preferred) or createJdbi()");
        }
    }

    private boolean isOverridden(String methodName) {
        try {
            getClass().getDeclaredMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    protected Jdbi createJdbi() {
        return Jdbi.create(getDataSource());
    }

    protected DataSource createDataSource() {
        throw new UnsupportedOperationException("JdbiRule must override createDataSource() to support this feature");
    }

    DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (mutex) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Create a JdbiRule with an embedded Postgres instance.
     * Your project must depend on the {@code otj-pg-embedded} artifact.
     */
    public static JdbiRule embeddedPostgres() {
        return new EmbeddedPostgresJdbiRule();
    }

    /**
     * Create a JdbiRule with an in-memory H2 database instance.
     * Your project must depend on the {@code h2} database artifact.
     */
    public static JdbiRule h2() {
        return new JdbiRule() {
            @Override
            protected DataSource createDataSource() {
                return JdbcConnectionPool.create("jdbc:h2:mem:" + UUID.randomUUID(), "", "");
            }
        };
    }

    /**
     * Run database migration scripts from {@code db/migration} on the classpath, using Flyway.
     * @return this
     */
    public JdbiRule migrateWithFlyway() {
        return migrateWithFlyway("db/migration");
    }

    /**
     * Run database migration scripts from the given location on the classpath, using Flyway.
     * @return this
     */
    public JdbiRule migrateWithFlyway(String... locations) {
        this.migrationScriptPaths = locations;
        return this;
    }

    /**
     * Discover and install plugins from the classpath.
     * @see JdbiRule#withPlugin(JdbiPlugin) we recommend installing plugins explicitly instead
     */
    public JdbiRule withPlugins() {
        installPlugins = true;
        return this;
    }

    /**
     * Install a plugin into JdbiRule.
     */
    public JdbiRule withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);
        return this;
    }

    @Override
    protected void before() throws Throwable {
        if (migrationScriptPaths != null) {
            Flyway flyway = new Flyway();
            flyway.setDataSource(getDataSource());
            flyway.setLocations(migrationScriptPaths);
            flyway.migrate();
        }

        jdbi = createJdbi();
        if (installPlugins) {
            jdbi.installPlugins();
        }
        plugins.forEach(jdbi::installPlugin);
        handle = jdbi.open();
    }

    @Override
    protected void after() {
        handle.close();
        jdbi = null;
        dataSource = null;
    }

    /**
     * Get Jdbi, in case you want to open additional handles to the same data source.
     */
    public Jdbi getJdbi() {
        return jdbi;
    }

    /**
     * Get the single Handle instance opened for the duration of this test case.
     */
    public Handle getHandle() {
        return handle;
    }

    /**
     * Attach an extension (such as a SqlObject) to the managed handle.
     */
    public <T> T attach(Class<T> extension) {
        return getHandle().attach(extension);
    }
}

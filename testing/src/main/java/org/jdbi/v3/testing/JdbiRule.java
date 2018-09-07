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

import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;
import java.util.concurrent.locks.ReentrantLock;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * JUnit {@code @Rule} to manage a Jdbi instance pointed to a managed database.
 */
public abstract class JdbiRule extends ExternalResource {
    
    private final List<JdbiPlugin> plugins = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private volatile DataSource dataSource;
    private boolean installPlugins;
    private Migration migration;
    private Handle handle;
    private Jdbi jdbi;
    
    protected abstract DataSource createDataSource();
    
    private DataSource getDataSource() {
        if (dataSource == null) {            
            try {
                lock.lock();
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            } finally {
                lock.unlock();
            }
        }
        return dataSource;
    }

    /**
     * Create a JdbiRule with an embedded PostgreSQL instance. Your project must
     * depend on the {@code otj-pg-embedded} artifact.
     */
    public static JdbiRule embeddedPostgres() {
        return new EmbeddedPostgresJdbiRule();
    }

    /**
     * Create a JdbiRule using an external PostgreSQL instance. Your project
     * must depend on the {@code postgresql} driver artifact.
     */
    public static JdbiRule externalPostgres(final String hostname, final Integer port,
            final String username, String password, String database) {
        return new ExternalPostgresJdbiRule(hostname, port, username, password, database);
    }

    /**
     * Create a JdbiRule with an in-memory H2 database instance. Your project
     * must depend on the {@code h2} database artifact.
     */
    public static JdbiRule h2() {
        return new EmbeddedH2JdbiRule();
    }

    /**
     * Run database migration.
     */
    public JdbiRule withMigration(final Migration migration) {
        this.migration = migration;
        return this;
    }

    /**
     * Discover and install plugins from the classpath.
     *
     * @see JdbiRule#withPlugin(JdbiPlugin) we recommend installing plugins
     * explicitly instead
     */
    public JdbiRule withPlugins() {
        installPlugins = true;
        return this;
    }

    /**
     * Install a plugin into JdbiRule.
     */
    public JdbiRule withPlugin(final JdbiPlugin plugin) {
        plugins.add(plugin);
        return this;
    }
    
    @Override
    protected void before() throws Throwable {
        if (migration != null) {
            final Flyway flyway = new Flyway();
            flyway.setDataSource(getDataSource());
            flyway.setLocations(migration.paths.toArray(new String[migration.paths.size()]));
            flyway.setSchemas(migration.schemas.toArray(new String[migration.schemas.size()]));
            flyway.migrate();
        }
        
        jdbi = Jdbi.create(getDataSource());
        if (installPlugins) {
            jdbi.installPlugins();
        }
        plugins.forEach(jdbi::installPlugin);
        handle = jdbi.open();
    }
    
    @Override
    protected void after() {
        if (migration != null && migration.cleanAfter) {
            final Flyway flyway = new Flyway();
            flyway.setDataSource(getDataSource());
            flyway.setLocations(migration.paths.toArray(new String[migration.paths.size()]));
            flyway.setSchemas(migration.schemas.toArray(new String[migration.schemas.size()]));
            flyway.clean();
        }
        handle.close();
        jdbi = null;
        dataSource = null;
    }

    /**
     * Get Jdbi, in case you want to open additional handles to the same data
     * source.
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
    public <T> T attach(final Class<T> extension) {
        return getHandle().attach(extension);
    }
}

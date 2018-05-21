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
import java.util.List;
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.rules.ExternalResource;

/**
 * JUnit {@code @Rule} to manage a Jdbi instance pointed to a managed database.
 */
public abstract class JdbiRule extends ExternalResource {

    private Jdbi jdbi;
    private Handle handle;
    private boolean installPlugins;
    private List<JdbiPlugin> plugins = new ArrayList<>();

    protected abstract Jdbi createJdbi();

    /**
     * Create a JdbiRule with an embedded Postgres instance.
     * Your project must depend on the {@code otj-pg-embedded} artifact.
     */
    public static JdbiRule embeddedPostgres() {
        return new EmbeddedPostgresJdbiRule();
    }

    /**
     * Create a JdbiRule with an embedded Postgres instance that will be prepared by Flyway.<br>
     * Your project must depend on the {@code otj-pg-embedded} artifact.
     * @param migrationLocations classpath locations to scan recursively for flyway migrations. (default: db/migration)
     */
    public static JdbiRule preparedEmbeddedPostgres(final String... migrationLocations) {
        return new PreparedEmbeddedPostgresJdbiRule(migrationLocations);
    }

    /**
     * Create a JdbiRule with an in-memory H2 database instance.
     * Your project must depend on the {@code h2} database artifact.
     */
    public static JdbiRule h2() {
        return new JdbiRule() {
            @Override
            protected Jdbi createJdbi() {
                return Jdbi.create("jdbc:h2:mem:" + UUID.randomUUID());
            }
        };
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

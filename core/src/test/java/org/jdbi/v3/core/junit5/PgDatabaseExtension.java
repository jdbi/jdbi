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
package org.jdbi.v3.core.junit5;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import de.softwareforge.testing.postgres.embedded.DatabaseInfo;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Postgres Test Support for core tests. Do NOT use this outside the core tests (or tests that use core tests), use the JdbiExtension from the
 * jdbi3-testing module instead!
 */
public final class PgDatabaseExtension implements DatabaseExtension<PgDatabaseExtension>, BeforeEachCallback, AfterEachCallback {

    private final EmbeddedPgExtension pg;
    private final boolean installPlugins;

    private final Set<JdbiPlugin> plugins = new LinkedHashSet<>();

    private Optional<DatabaseInitializer> initializerMaybe = Optional.empty();

    private DatabaseInfo info = null;
    private Jdbi jdbi = null;
    private Handle sharedHandle = null;

    public static PgDatabaseExtension instance(EmbeddedPgExtension pg) {
        return new PgDatabaseExtension(pg, false);
    }

    public static PgDatabaseExtension withPlugins(EmbeddedPgExtension pg) {
        return new PgDatabaseExtension(pg, true);
    }

    private PgDatabaseExtension(EmbeddedPgExtension pg, boolean installPlugins) {
        this.pg = pg;
        this.installPlugins = installPlugins;
    }

    @Override
    public Jdbi getJdbi() {
        if (jdbi == null) {
            throw new IllegalStateException("jdbi is null!");
        }
        return jdbi;
    }

    @Override
    public String getUri() {
        return info.asJdbcUrl();
    }

    @Override
    public Handle getSharedHandle() {
        if (sharedHandle == null) {
            throw new IllegalStateException("sharedHandle is null!");
        }
        return sharedHandle;
    }

    @Override
    public PgDatabaseExtension withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);

        return this;
    }

    @Override
    public PgDatabaseExtension withInitializer(DatabaseInitializer initializer) {
        if (this.initializerMaybe.isPresent()) {
            throw new IllegalStateException("Initializer already set!");
        }
        this.initializerMaybe = Optional.of(initializer);

        return this;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (info != null) {
            throw new IllegalStateException("info is not null!");
        }

        info = pg.createDatabaseInfo();

        jdbi = Jdbi.create(info.asDataSource());

        if (installPlugins) {
            jdbi.installPlugins();
        }

        plugins.forEach(jdbi::installPlugin);
        sharedHandle = jdbi.open();

        initializerMaybe.ifPresent(i -> i.initialize(sharedHandle));
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (sharedHandle == null) {
            throw new IllegalStateException("shared handle was not initialized!");
        }

        this.jdbi = null;
        this.sharedHandle.close();
    }
}

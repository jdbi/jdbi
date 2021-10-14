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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class SqliteDatabaseExtension implements DatabaseExtension<SqliteDatabaseExtension>, BeforeEachCallback, AfterEachCallback {

    private static final DatabaseInitializer SOMETHING_INITIALIZER =
        h -> h.execute("create table something (id identity primary key, name varchar(50), integerValue integer, intValue integer)");

    private final String uri = "jdbc:sqlite::memory:";
    private final Set<JdbiPlugin> plugins = new LinkedHashSet<>();

    private final boolean installPlugins;
    private Optional<DatabaseInitializer> initializerMaybe = Optional.empty();

    private Jdbi jdbi = null;
    private Handle sharedHandle = null;

    public static SqliteDatabaseExtension instance() {
        return new SqliteDatabaseExtension(false);
    }

    public static SqliteDatabaseExtension withPlugins() {
        return new SqliteDatabaseExtension(true);
    }

    @Deprecated
    public static SqliteDatabaseExtension withSomething() {
        return instance().withInitializer(SOMETHING_INITIALIZER);
    }

    private SqliteDatabaseExtension(boolean installPlugins) {
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
        return uri;
    }

    @Override
    public Handle getSharedHandle() {
        if (sharedHandle == null) {
            throw new IllegalStateException("sharedHandle is null!");
        }
        return sharedHandle;
    }

    @Override
    public SqliteDatabaseExtension withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);

        return this;
    }

    @Override
    public SqliteDatabaseExtension withInitializer(DatabaseInitializer initializer) {
        if (this.initializerMaybe.isPresent()) {
            throw new IllegalStateException("Initializer already set!");
        }
        this.initializerMaybe = Optional.of(initializer);

        return this;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (jdbi != null) {
            throw new IllegalStateException("jdbi is not null!");
        }
        jdbi = Jdbi.create(uri);

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

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
import java.util.UUID;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * H2 Test Support for core tests. Do NOT use this outside the core tests (or tests that use core tests), use the JdbiExtension from the
 * jdbi3-testing module instead!
 */
public final class H2DatabaseExtension implements DatabaseExtension<H2DatabaseExtension>, BeforeEachCallback, AfterEachCallback {

    public static final DatabaseInitializer SOMETHING_INITIALIZER =
        h -> h.execute("create table something (id identity primary key, name varchar(50), integerValue integer, intValue integer)");

    private final String uri = "jdbc:h2:mem:" + UUID.randomUUID();
    private final Set<JdbiPlugin> plugins = new LinkedHashSet<>();

    private final boolean installPlugins;
    private Optional<DatabaseInitializer> initializerMaybe = Optional.empty();

    private Jdbi jdbi = null;
    private Handle sharedHandle = null;

    public static H2DatabaseExtension instance() {
        return new H2DatabaseExtension(false);
    }

    public static H2DatabaseExtension withPlugins() {
        return new H2DatabaseExtension(true);
    }

    @Deprecated
    public static H2DatabaseExtension withSomething() {
        return instance().withInitializer(SOMETHING_INITIALIZER);
    }

    private H2DatabaseExtension(boolean installPlugins) {
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
    public H2DatabaseExtension withPlugin(JdbiPlugin plugin) {
        plugins.add(plugin);

        return this;
    }

    @Override
    public H2DatabaseExtension withInitializer(DatabaseInitializer initializer) {
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

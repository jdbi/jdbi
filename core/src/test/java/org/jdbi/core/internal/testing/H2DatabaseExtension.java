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
package org.jdbi.core.internal.testing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jdbi.core.Handle;
import org.jdbi.core.Handles;
import org.jdbi.core.Jdbi;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.SqlStatements;
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

    public static final DatabaseInitializer USERS_INITIALIZER = h -> {
        h.execute("CREATE TABLE users (id INTEGER PRIMARY KEY, name VARCHAR)");
        h.execute("INSERT INTO users VALUES (1, 'Alice')");
        h.execute("INSERT INTO users VALUES (2, 'Bob')");
    };

    private final String uri = "jdbc:h2:mem:" + UUID.randomUUID();
    private final Set<JdbiPlugin> plugins = new LinkedHashSet<>();
    private final JdbiLeakChecker leakChecker = new JdbiLeakChecker();

    private Optional<DatabaseInitializer> initializerMaybe = Optional.empty();

    private volatile Connection lastConnection = null;

    private Jdbi jdbi = null;
    private Handle sharedHandle = null;
    private boolean enableLeakchecker = true;

    public static H2DatabaseExtension instance() {
        return new H2DatabaseExtension();
    }

    @Deprecated
    public static H2DatabaseExtension withSomething() {
        return instance().withInitializer(SOMETHING_INITIALIZER);
    }

    private H2DatabaseExtension() {}

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

    /**
     * Returns the last connection handed out by this extension. This is <b>not</b> a general
     * purpose API but is intended for tests that need to compare connection objects between a
     * handle and what the extension has handed out.
     * <br>
     * This API is <b>not multi-thread safe</b> and will not work if the extension is shared between
     * multiple threads.
     * @return A connection object. Can be null if no connection had been handed out.
     */
    public Connection getLastConnection() {
        return this.lastConnection;
    }

    public void clearLastConnection() {
        this.lastConnection = null;
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
    public H2DatabaseExtension withoutLeakChecker() {
        this.enableLeakchecker = false;
        return this;
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        if (jdbi != null) {
            throw new IllegalStateException("jdbi is not null!");
        }
        jdbi = Jdbi.create(() -> {
            final Connection connection = DriverManager.getConnection(uri);
            // this will only work reliably in single-threaded tests. Any multi-threaded
            // test will hand out the last connection used by the last thread calling this
            // method.
            this.lastConnection = connection;
            return connection;
        });

        installTestPlugins(jdbi);

        if (enableLeakchecker) {
            jdbi.getConfig(Handles.class).addListener(leakChecker);
            jdbi.getConfig(SqlStatements.class).addContextListener(leakChecker);
        }

        plugins.forEach(jdbi::installPlugin);
        sharedHandle = jdbi.open();

        initializerMaybe.ifPresent(i -> i.initialize(sharedHandle));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (sharedHandle == null) {
            throw new IllegalStateException("shared handle was not initialized!");
        }

        this.jdbi = null;
        this.sharedHandle.close();

        if (enableLeakchecker) {
            leakChecker.checkForLeaks();
        }
    }
}

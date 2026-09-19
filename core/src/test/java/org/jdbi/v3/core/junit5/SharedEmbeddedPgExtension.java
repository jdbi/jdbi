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

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.StoreScope;

/**
 * Starts one embedded postgres server for the whole test run and hands every test class its own
 * database on it. Register it next to the JdbiExtension and pass
 * {@link #instance()} to the extension:
 *
 * <pre>{@code
 * @RegisterExtension
 * public static SharedEmbeddedPgExtension sharedPg = new SharedEmbeddedPgExtension();
 *
 * @RegisterExtension
 * public JdbiExtension pgExtension = JdbiExtension.postgres(SharedEmbeddedPgExtension.instance());
 * }</pre>
 *
 * <p>
 * {@link EmbeddedPgExtension} keys its server off the store of the context it is registered on,
 * which is the test class, so registering the same instance on many classes still starts a server
 * for each one. Surefire also builds a fresh root context per test class, so the root store is no
 * help either. This class keeps the server in the launcher session store, which outlives both: the
 * first class that runs starts the server and every later class reuses it. The store closes the
 * server when the launcher session ends.
 * </p>
 * <p>
 * Surefire forks several JVMs and each fork runs its own launcher session, so this yields one
 * server per fork rather than one per test class.
 * </p>
 * <p>
 * A class that changes something the whole server owns, such as an initdb setting, still needs its
 * own instance. {@code TestPostgresTypes} forces a locale and therefore builds its own.
 * </p>
 */
public final class SharedEmbeddedPgExtension implements BeforeAllCallback {

    private static final Object SERVER_KEY = new Object();
    private static final Namespace NAMESPACE = Namespace.create(SharedEmbeddedPgExtension.class);

    private static final EmbeddedPgExtension PG = MultiDatabaseBuilder.instanceWithDefaults().build();

    /**
     * Returns the shared server. Valid once a class that registers this extension has started.
     *
     * @return the shared embedded postgres extension.
     */
    public static EmbeddedPgExtension instance() {
        return PG;
    }

    /**
     * Returns the shared server, starting it if this is the first use in the launcher session. Use
     * this from another extension that has a context but does not want to make its callers register
     * this extension separately.
     *
     * @param context the extension context of the caller.
     * @return the shared embedded postgres extension.
     */
    public static EmbeddedPgExtension instance(ExtensionContext context) {
        // computeIfAbsent does not keep a creator that threw, unlike getOrComputeIfAbsent, so a
        // failed start fails only the current class and the next class starts the server again
        context.getStore(StoreScope.LAUNCHER_SESSION, NAMESPACE)
            .computeIfAbsent(SERVER_KEY, key -> new ServerLifecycle(context.getRoot()), ServerLifecycle.class);
        return PG;
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        instance(context);
    }

    /**
     * Starts the server on creation and stops it when the launcher session ends. The store closes an
     * {@link AutoCloseable} value for us, so the server outlives every test class and is still shut
     * down cleanly.
     */
    static final class ServerLifecycle implements AutoCloseable {

        private final ExtensionContext root;

        ServerLifecycle(ExtensionContext root) {
            this.root = root;
            try {
                PG.beforeAll(root);
            } catch (Exception e) {
                throw new IllegalStateException("could not start the shared embedded postgres server", e);
            }
        }

        @Override
        public void close() throws Exception {
            PG.afterAll(root);
        }
    }
}

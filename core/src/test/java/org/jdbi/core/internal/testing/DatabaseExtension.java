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

import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfiguringPlugin;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.spi.JdbiPlugin;

public interface DatabaseExtension<T extends DatabaseExtension<T>> {

    Jdbi getJdbi();

    /**
     * Returns a {@link Jdbi.Builder} pre-configured with this extension's connection source, installed plugins, and
     * (unless disabled with {@link #withoutLeakChecker()}) its leak checker, so a test can add per-test configuration
     * (a transaction handler, a spy, and the like) and build its own {@link Jdbi} that still participates in the
     * extension's leak-check lifecycle.
     *
     * @return a builder assembling a {@code Jdbi} equivalent to this extension's.
     */
    Jdbi.Builder builder() throws Exception;

    String getUri();

    T withPlugin(JdbiPlugin plugin);

    T withInitializer(DatabaseInitializer initializer);

    T withoutLeakChecker();

    Handle getSharedHandle();

    default Handle openHandle() {
        return getJdbi().open();
    }

    default <C extends JdbiConfig<C>> T withConfig(Class<C> configClass, UnaryOperator<C> configurer) {
        return withPlugin(ConfiguringPlugin.of(configClass, configurer));
    }

    default T withConfig(Consumer<Jdbi.Builder> configurer) {
        return withPlugin(JdbiPlugin.of(configurer));
    }

    @SuppressWarnings("unchecked")
    default void installTestPlugins(Jdbi.Builder builder) throws Exception {
        // cache plugin tests
        String cachePluginName = System.getProperty("jdbi.test.cache-plugin");
        if (cachePluginName != null) {
            Class<? extends JdbiPlugin> cachePluginClass = (Class<? extends JdbiPlugin>) Class.forName(cachePluginName);
            JdbiPlugin cachePlugin = cachePluginClass.getConstructor().newInstance();
            builder.installPlugin(cachePlugin);
        }
    }

    @FunctionalInterface
    interface DatabaseInitializer {
        void initialize(Handle handle);
    }
}

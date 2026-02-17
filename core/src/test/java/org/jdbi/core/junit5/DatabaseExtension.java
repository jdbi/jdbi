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
package org.jdbi.core.junit5;

import java.util.function.Consumer;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfiguringPlugin;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.spi.JdbiPlugin;

public interface DatabaseExtension<T extends DatabaseExtension<T>> {

    Jdbi getJdbi();

    String getUri();

    T withPlugin(JdbiPlugin plugin);

    T withInitializer(DatabaseInitializer initializer);

    T withoutLeakChecker();

    Handle getSharedHandle();

    default Handle openHandle() {
        return getJdbi().open();
    }

    default <C extends JdbiConfig<C>> T withConfig(Class<C> configClass, Consumer<C> configurer) {
        return withPlugin(ConfiguringPlugin.of(configClass, configurer));
    }

    @SuppressWarnings("unchecked")
    default void installTestPlugins(Jdbi jdbi) throws Exception {
        // cache plugin tests
        String cachePluginName = System.getProperty("jdbi.test.cache-plugin");
        if (cachePluginName != null) {
            Class<? extends JdbiPlugin> cachePluginClass = (Class<? extends JdbiPlugin>) Class.forName(cachePluginName);
            JdbiPlugin cachePlugin = cachePluginClass.getConstructor().newInstance();
            jdbi.installPlugin(cachePlugin);
        }
    }

    @FunctionalInterface
    interface DatabaseInitializer {
        void initialize(Handle handle);
    }
}

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
package org.jdbi.v3.core.config;

import java.util.Map;
import java.util.WeakHashMap;

import static java.util.Collections.synchronizedMap;

/**
 * A registry of {@link JdbiConfig} instances by type.
 *
 * @see Configurable
 */
public class ConfigRegistry {
    private final Map<Class<? extends JdbiConfig<?>>, JdbiConfig<?>> cache = synchronizedMap(new WeakHashMap<>());

    /**
     * Creates a new config registry.
     */
    public ConfigRegistry() {}

    private ConfigRegistry(ConfigRegistry that) {
        that.cache.forEach((type, config) -> {
            JdbiConfig<?> copy = config.createCopy();
            copy.setRegistry(this);
            cache.put(type, copy);
        });
    }

    /**
     * Returns this registry's instance of the given config class. Creates an instance on-demand if this registry does
     * not have one of the given type yet.
     *
     * @param configClass the config class type.
     * @param <C>         the config class type.
     * @return the given config class instance that belongs to this registry.
     */
    public <C extends JdbiConfig<C>> C get(Class<C> configClass) {
        return configClass.cast(cache.computeIfAbsent(configClass, type -> {
            try {
                C config = configClass.getDeclaredConstructor().newInstance();
                config.setRegistry(this);
                return config;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to instantiate config class " + configClass
                    + ". Is there a public no-arg constructor?", e);
            }
        }));
    }

    /**
     * Returns a copy of this config registry.
     *
     * @return a copy of this config registry
     * @see JdbiConfig#createCopy() config objects in the returned registry are copies of the corresponding
     * config objects from this registry.
     */
    public ConfigRegistry createCopy() {
        return new ConfigRegistry(this);
    }
}

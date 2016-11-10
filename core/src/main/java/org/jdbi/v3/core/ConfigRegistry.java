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
package org.jdbi.v3.core;

import static java.util.Collections.synchronizedMap;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import org.jdbi.v3.core.extension.JdbiConfig;

public class ConfigRegistry {
    private final Optional<ConfigRegistry> parent;
    private final Map<Class<? extends JdbiConfig>, JdbiConfig<?>> cache = synchronizedMap(new WeakHashMap<>());

    ConfigRegistry() {
        parent = Optional.empty();
    }

    private ConfigRegistry(ConfigRegistry that) {
        parent = Optional.of(that);
    }

    @SuppressWarnings("unchecked")
    <C extends JdbiConfig<C>> C get(Class<C> configClass) {
        return (C) cache.computeIfAbsent(configClass, c ->
                createFromParent(configClass)
                        .orElseGet(() -> create(configClass)));
    }

    private <C extends JdbiConfig<C>> Optional<C> createFromParent(Class<C> configClass) {
        return parent.map(p -> p.get(configClass).createChild());
    }

    private static <C extends JdbiConfig<C>> C create(Class<C> configClass) {
        try {
            return configClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to instantiate config class " + configClass +
                    ". Is there a public no-arg constructor?");
        }
    }

    public ConfigRegistry createChild() {
        return new ConfigRegistry(this);
    }
}

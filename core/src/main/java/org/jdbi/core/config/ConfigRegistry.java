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
package org.jdbi.core.config;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jdbi.core.argument.Arguments;
import org.jdbi.core.collector.JdbiCollectors;
import org.jdbi.core.config.internal.ConfigCaches;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.Mappers;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.ConfigReader;

/**
 * A registry of {@link JdbiConfig} instances by type.
 *
 * @see Configurable
 */
public final class ConfigRegistry implements ConfigReader {

    private static final Class<?>[] JDBI_CONFIG_TYPES = {ConfigRegistry.class};

    private final Map<Class<? extends JdbiConfig<?>>, JdbiConfig<?>> configs = new ConcurrentHashMap<>(32);
    private final Map<Class<? extends JdbiConfig<?>>, Function<ConfigRegistry, JdbiConfig<?>>> configFactories;

    /**
     * Creates a new config registry.
     */
    public ConfigRegistry() {
        configFactories = new ConcurrentHashMap<>();
        get(ConfigCaches.class);
        get(SqlStatements.class);
        get(Arguments.class);
        get(RowMappers.class);
        get(ColumnMappers.class);
        get(Mappers.class);
        get(JdbiCollectors.class);
    }

    private ConfigRegistry(ConfigRegistry that) {
        configFactories = that.configFactories;
        that.configs.forEach((type, config) -> {
            JdbiConfig<?> copy = config.createCopy();
            configs.put(type, copy);
        });
        configs.values().forEach(c -> c.setRegistry(this));
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
        // we would computeIfAbsent if not for JDK-8062841 >:(
        final JdbiConfig<?> lookup = configs.get(configClass);
        if (lookup != null) {
            return configClass.cast(lookup);
        }
        C config = configClass.cast(configFactory(configClass).apply(this));
        return Optional.ofNullable(configClass.cast(configs.putIfAbsent(configClass, config))).orElse(config);
    }

    private Function<ConfigRegistry, JdbiConfig<?>> configFactory(Class<? extends JdbiConfig<?>> configClass) {
        return configFactories.computeIfAbsent(configClass, klass -> {
            var handleHolder = JdbiClassUtils.findConstructor(klass, JDBI_CONFIG_TYPES);
            return registry -> {
                var config = handleHolder.invoke(handle -> handle.invokeExact(registry));
                config.setRegistry(registry);
                return config;
            };
        });
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

    @Override
    public ConfigRegistry getConfig() {
        return this;
    }
}

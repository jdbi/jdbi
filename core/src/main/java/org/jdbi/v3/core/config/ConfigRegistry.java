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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.Mappers;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.meta.Alpha;

/**
 * A registry of {@link JdbiConfig} instances by type.
 *
 * <p>A registry returned by {@link #createCopy()} materializes each config object lazily: the first
 * {@link #get(Class)} for a config type copies the source registry's instance at that moment. Once
 * materialized, the copy is private to this registry, so changes to it do not modify the source and
 * vice-versa. Configuration is expected to be finalized before Jdbi is used; a source registry that
 * is reconfigured between {@code createCopy()} and the copy's first access of the changed config type
 * is observed with the new value. Call {@link #setEagerCopies(boolean)} to restore copy-at-creation
 * timing instead.
 *
 * @see Configurable
 */
public final class ConfigRegistry {

    private static final Class<?>[] JDBI_CONFIG_TYPES = {ConfigRegistry.class};

    private final Map<Class<? extends JdbiConfig<?>>, JdbiConfig<?>> configs = new ConcurrentHashMap<>(32);
    private final Map<Class<? extends JdbiConfig<?>>, Function<ConfigRegistry, JdbiConfig<?>>> configFactories;
    private final ConfigRegistry source;
    private final AtomicBoolean eagerCopies = new AtomicBoolean();

    /**
     * Creates a new config registry.
     */
    public ConfigRegistry() {
        source = null;
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
        final boolean eager = that.eagerCopies.get();
        eagerCopies.set(eager);
        if (eager) {
            source = null;
            for (Class<? extends JdbiConfig<?>> type : that.knownConfigTypes()) {
                configs.put(type, that.lookup(type).createCopy());
            }
            configs.values().forEach(c -> c.setRegistry(this));
        } else {
            source = that;
        }
    }

    /**
     * Returns this registry's instance of the given config class. Creates an instance on-demand if this registry does
     * not have one of the given type yet, copying the source registry's instance if this registry is a copy.
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
        final C config;
        if (source != null) {
            config = configClass.cast(source.get(configClass).createCopy());
            config.setRegistry(this);
        } else {
            config = configClass.cast(configFactory(configClass).apply(this));
        }
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
     * Returns the config types this registry knows about: its own materialized configs plus, for a lazy
     * copy, every type known to the registries it copies from.
     */
    private Set<Class<? extends JdbiConfig<?>>> knownConfigTypes() {
        final Set<Class<? extends JdbiConfig<?>>> types = new LinkedHashSet<>();
        for (ConfigRegistry registry = this; registry != null; registry = registry.source) {
            types.addAll(registry.configs.keySet());
        }
        return types;
    }

    /**
     * Returns the effective config instance for the given type without materializing a copy: the nearest
     * instance along the source chain, or null if no registry in the chain has one.
     */
    private JdbiConfig<?> lookup(Class<? extends JdbiConfig<?>> configClass) {
        for (ConfigRegistry registry = this; registry != null; registry = registry.source) {
            final JdbiConfig<?> config = registry.configs.get(configClass);
            if (config != null) {
                return config;
            }
        }
        return null;
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

    /**
     * Controls when {@link #createCopy()} copies the config objects. The default (false) materializes each
     * config object on its first access in the copy. Set true to copy every config object at
     * {@code createCopy()} time, the behavior of Jdbi releases before 3.55.0. Use this only if code depends
     * on the exact moment a copy is taken; it restores the old allocation cost on every copy.
     *
     * <p>Copies inherit the mode this registry has when {@code createCopy()} runs. Set the flag on the
     * {@link org.jdbi.v3.core.Jdbi} registry before handles or extensions are created.
     *
     * @param eagerCopies true to copy config objects at {@code createCopy()} time
     */
    @Alpha
    public void setEagerCopies(boolean eagerCopies) {
        this.eagerCopies.set(eagerCopies);
    }

    /**
     * Returns true if {@link #createCopy()} copies all config objects immediately.
     *
     * @return true if {@link #createCopy()} copies all config objects immediately
     */
    @Alpha
    public boolean isEagerCopies() {
        return eagerCopies.get();
    }
}

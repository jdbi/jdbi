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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.collector.JdbiCollectors;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.Mappers;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.statement.SqlStatements;

/**
 * A registry of {@link JdbiConfig} instances by type.
 *
 * @see Configurable
 */
public final class ConfigRegistry {
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
            final Exception notFound;
            try {
                MethodHandle mh = MethodHandles.lookup().findConstructor(klass,
                        MethodType.methodType(void.class, ConfigRegistry.class))
                    .asType(MethodType.methodType(JdbiConfig.class, ConfigRegistry.class));
                return registry -> {
                    try {
                        return (JdbiConfig<?>) mh.invokeExact(registry);
                    } catch (Throwable e) {
                        throw Sneaky.throwAnyway(e);
                    }
                };
            } catch (NoSuchMethodException e) {
                notFound = e;
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to use constructor taking ConfigRegistry to create " + configClass, e);
            }
            try {
                MethodHandle mh = MethodHandles.lookup().findConstructor(klass,
                        MethodType.methodType(void.class))
                    .asType(MethodType.methodType(JdbiConfig.class));
                return registry -> {
                    JdbiConfig<?> result;
                    try {
                        result = (JdbiConfig<?>) mh.invokeExact();
                    } catch (Throwable e) {
                        throw Sneaky.throwAnyway(e);
                    }
                    result.setRegistry(registry);
                    return result;
                };
            } catch (ReflectiveOperationException e) {
                IllegalStateException failure = new IllegalStateException("Unable to instantiate config class " + configClass
                        + ". Is there a public no-arg constructor?", e);
                failure.addSuppressed(notFound);
                throw failure;
            }
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
}

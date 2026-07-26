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
package org.jdbi.core.mapper.reflect.internal;

import java.util.HashMap;
import java.util.Map;

import org.jdbi.core.config.JdbiConfig;

/**
 * Registry of {@link PojoPropertiesFactory} instances. Holds only registration data; resolving a type into
 * its {@link PojoProperties} is done per configuration registry by {@link PojoResolver}.
 * <p>
 * This configuration is immutable: {@link #register} returns a new instance, leaving the receiver unchanged.
 */
public final class PojoTypes implements JdbiConfig<PojoTypes> {
    private final Map<Class<?>, PojoPropertiesFactory> factories;

    public PojoTypes() {
        this(Map.of());
    }

    private PojoTypes(final Map<Class<?>, PojoPropertiesFactory> factories) {
        this.factories = factories;
    }

    public PojoTypes register(final Class<?> key, final PojoPropertiesFactory factory) {
        final Map<Class<?>, PojoPropertiesFactory> updated = new HashMap<>(factories);
        updated.put(key, factory);
        return new PojoTypes(Map.copyOf(updated));
    }

    /**
     * Returns the registered factories keyed by erased type. Consumed by {@link PojoResolver}.
     *
     * @return the registered pojo-properties factories
     */
    Map<Class<?>, PojoPropertiesFactory> getFactories() {
        return factories;
    }

    @Override
    public PojoTypes createCopy() {
        // Immutable: safe to share across registries.
        return this;
    }
}

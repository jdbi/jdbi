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

import java.util.Map;

import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.internal.CopyOnWriteHashMap;

/**
 * Registry of {@link PojoPropertiesFactory} instances. Holds only registration data; resolving a type into
 * its {@link PojoProperties} is done per configuration registry by {@link PojoResolver}.
 */
public class PojoTypes implements JdbiConfig<PojoTypes> {
    private final Map<Class<?>, PojoPropertiesFactory> factories;

    public PojoTypes() {
        factories = new CopyOnWriteHashMap<>();
    }

    private PojoTypes(PojoTypes other) {
        factories = new CopyOnWriteHashMap<>(other.factories);
    }

    public PojoTypes register(Class<?> key, PojoPropertiesFactory factory) {
        factories.put(key, factory);
        return this;
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
        return new PojoTypes(this);
    }
}

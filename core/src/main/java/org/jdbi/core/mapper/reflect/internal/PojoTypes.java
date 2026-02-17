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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.core.generic.GenericTypes;
import org.jdbi.core.internal.CopyOnWriteHashMap;

public class PojoTypes implements JdbiConfig<PojoTypes> {
    private final Map<Class<?>, PojoPropertiesFactory> factories;
    private ConfigRegistry registry;

    public PojoTypes() {
        factories = new CopyOnWriteHashMap<>();
    }

    private PojoTypes(PojoTypes other) {
        factories = new CopyOnWriteHashMap<>(other.factories);
    }

    @Override
    public void setRegistry(ConfigRegistry registry) {
        this.registry = registry;
    }

    public PojoTypes register(Class<?> key, PojoPropertiesFactory factory) {
        factories.put(key, factory);
        return this;
    }

    public Optional<PojoProperties<?>> findFor(Type type) {
        return Optional.ofNullable(factories.get(GenericTypes.getErasedType(type)))
                .map(ppf -> ppf.create(type, registry));
    }

    @Override
    public PojoTypes createCopy() {
        return new PojoTypes(this);
    }
}

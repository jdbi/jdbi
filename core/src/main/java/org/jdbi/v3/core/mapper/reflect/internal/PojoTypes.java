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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.generic.GenericTypes;

public class PojoTypes implements JdbiConfig<PojoTypes> {
    private final Map<Class<?>, Function<Type, PojoProperties<?>>> factories = new HashMap<>();

    public PojoTypes() {}

    private PojoTypes(PojoTypes other) {
        factories.putAll(other.factories);
    }

    public PojoTypes register(Class<?> key, Function<Type, PojoProperties<?>> factory) {
        factories.put(key, factory);
        return this;
    }

    public Optional<PojoProperties<?>> findFor(Type type) {
        return Optional.ofNullable(factories.get(GenericTypes.getErasedType(type)))
                .map(ppf -> ppf.apply(type));
    }

    @Override
    public PojoTypes createCopy() {
        return new PojoTypes(this);
    }
}

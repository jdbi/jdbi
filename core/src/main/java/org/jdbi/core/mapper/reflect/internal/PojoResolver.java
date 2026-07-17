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
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericTypes;

/**
 * Resolves {@link PojoProperties} for a specific {@link ConfigRegistry}.
 * <p>
 * A resolver reads the registered factories from the registry's {@link PojoTypes} (which holds only
 * registration data) and turns a type into its {@link PojoProperties}. It is obtained per registry via
 * {@link #forRegistry(ConfigRegistry)} and holds the registry reference the factories need; because it is
 * never shared across registry copies, that reference is always the one this resolver resolves against.
 */
public final class PojoResolver {

    /**
     * Returns the pojo-properties resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized pojo-properties resolver
     */
    public static PojoResolver forRegistry(final ConfigRegistry config) {
        return config.readAs(PojoResolver.class, PojoResolver::new);
    }

    private final ConfigRegistry registry;

    private PojoResolver(final ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Obtain the {@link PojoProperties} for the given type.
     *
     * @param type the type
     * @return the pojo properties for the given type, if a factory is registered for it.
     */
    public Optional<PojoProperties<?>> findFor(final Type type) {
        return Optional.ofNullable(registry.get(PojoTypes.class).getFactories().get(GenericTypes.getErasedType(type)))
                .map(ppf -> ppf.create(type, registry));
    }
}

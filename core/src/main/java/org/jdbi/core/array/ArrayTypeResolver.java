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
package org.jdbi.core.array;

import java.lang.reflect.Type;
import java.util.Optional;

import org.jdbi.core.config.ConfigView;

/**
 * Resolves {@link SqlArrayType}s for a specific {@link ConfigView}.
 * <p>
 * A resolver reads the registered factories from the registry's {@link SqlArrayTypes} (which holds only
 * registration data) and turns an array element type into a {@link SqlArrayType}. It is obtained per registry
 * via {@link #forRegistry(ConfigView)} and holds the registry reference the factories need; because it is
 * never shared across registry copies, that reference is always the one this resolver resolves against.
 */
public final class ArrayTypeResolver {

    /**
     * Returns the array-type resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized array-type resolver
     */
    public static ArrayTypeResolver forRegistry(final ConfigView config) {
        return config.readAs(ArrayTypeResolver.class, ArrayTypeResolver::new);
    }

    private final ConfigView registry;

    private ArrayTypeResolver(final ConfigView registry) {
        this.registry = registry;
    }

    /**
     * Obtain an {@link SqlArrayType} for the given array element type.
     *
     * @param elementType the array element type.
     * @return an {@link SqlArrayType} for the given element type.
     */
    public Optional<SqlArrayType<?>> findFor(final Type elementType) {
        return registry.get(SqlArrayTypes.class).getFactories().stream()
                .flatMap(factory -> factory.build(elementType, registry).stream())
                .findFirst();
    }
}

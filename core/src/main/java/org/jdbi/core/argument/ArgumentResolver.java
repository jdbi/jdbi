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
package org.jdbi.core.argument;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.meta.Beta;

/**
 * Resolves arguments for a specific {@link ConfigRegistry}, caching the results.
 * <p>
 * A resolver reads the registered factories from the registry's {@link Arguments} (which holds only
 * registration data) and turns a bound value into an {@link Argument}, memoizing the prepared factory
 * function per type. It is obtained per registry via {@link #forRegistry(ConfigRegistry)} and is scoped
 * to that registry: because it is never shared across registry copies, it safely holds the registry
 * reference, and its prepared-factory cache is warm across the many statements executed against a shared
 * registry, yet a forked registry starts with an empty cache and re-resolves against its own factories.
 */
public final class ArgumentResolver {

    /**
     * Returns the argument resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized argument resolver
     */
    public static ArgumentResolver forRegistry(final ConfigRegistry config) {
        return config.readAs(ArgumentResolver.class, ArgumentResolver::new);
    }

    private final ConfigRegistry registry;
    private final Map<QualifiedType<?>, Function<Object, Argument>> preparedFactories = new ConcurrentHashMap<>();
    private final Set<QualifiedType<?>> didPrepare = ConcurrentHashMap.newKeySet();

    // Registration only ever adds factories, so a change in factory count means a factory was registered
    // on this (still-mutable) registry after we cached; drop the stale cache. Once registration forks the
    // registry (immutable-config step), each fork has its own fresh resolver and this guard is moot.
    private volatile int factoryCount = -1;

    private ArgumentResolver(final ConfigRegistry registry) {
        this.registry = registry;
    }

    /**
     * Obtain an argument for the given value.
     *
     * @param type  the type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findFor(final Type type, final Object value) {
        return findFor(QualifiedType.of(type), value);
    }

    /**
     * Obtain an argument for the given value.
     *
     * @param type  the qualified type of the argument.
     * @param value the argument value.
     * @return an Argument for the given value.
     */
    public Optional<Argument> findFor(final QualifiedType<?> type, final Object value) {
        final List<QualifiedArgumentFactory> factories = factories();

        final Function<Object, Argument> prepared = preparedFactories.get(type);
        if (prepared != null) {
            return Optional.of(prepared.apply(value));
        }
        for (final QualifiedArgumentFactory factory : factories) {
            final Optional<Argument> maybeBuilt = factory.build(type, value, registry);
            if (maybeBuilt.isPresent()) {
                if (factory instanceof QualifiedArgumentFactory.Preparable p && didPrepare.add(type)) {
                    p.prepare(type, registry).ifPresent(argumentFactory ->
                            preparedFactories.putIfAbsent(type, argumentFactory));
                }
                return maybeBuilt;
            }
        }
        return Optional.empty();
    }

    /**
     * Obtain a prepared argument function for the given type.
     *
     * @param type the type of the argument.
     * @return an Argument factory function for the given type.
     */
    public Optional<Function<Object, Argument>> prepareFor(final Type type) {
        return prepareFor(QualifiedType.of(type));
    }

    /**
     * Obtain a prepared argument function for the given type.
     *
     * @param type the qualified type of the argument.
     * @return an Argument factory function for the given type.
     */
    @Beta
    public Optional<Function<Object, Argument>> prepareFor(final QualifiedType<?> type) {
        if (!registry.get(Arguments.class).isPreparedArgumentsEnabled()) {
            return Optional.empty();
        }
        final List<QualifiedArgumentFactory> factories = factories();

        final Function<Object, Argument> prepared = preparedFactories.get(type);
        if (prepared != null) {
            return Optional.of(prepared);
        }
        for (final QualifiedArgumentFactory factory : factories) {
            if (factory instanceof QualifiedArgumentFactory.Preparable preparable) {
                final Optional<Function<Object, Argument>> argumentFactory = preparable.prepare(type, registry);
                if (argumentFactory.isPresent()) {
                    preparedFactories.putIfAbsent(type, argumentFactory.get());
                    return argumentFactory;
                }
            }
        }
        return Optional.empty();
    }

    private List<QualifiedArgumentFactory> factories() {
        final List<QualifiedArgumentFactory> factories = registry.get(Arguments.class).getFactories();
        if (factories.size() != factoryCount) {
            preparedFactories.clear();
            didPrepare.clear();
            factoryCount = factories.size();
        }
        return factories;
    }
}

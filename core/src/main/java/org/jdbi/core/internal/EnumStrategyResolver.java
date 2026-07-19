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
package org.jdbi.core.internal;

import java.util.Optional;

import org.jdbi.core.config.ConfigView;
import org.jdbi.core.enums.EnumByName;
import org.jdbi.core.enums.EnumByOrdinal;
import org.jdbi.core.enums.EnumStrategy;
import org.jdbi.core.enums.Enums;
import org.jdbi.core.qualifier.QualifiedType;
import org.jdbi.core.qualifier.Qualifiers;

import static org.jdbi.core.generic.GenericTypes.getErasedType;

/**
 * Resolves the {@link EnumStrategy} to use for an enum type against a specific {@link ConfigView}.
 * <p>
 * This reads the registry's {@link Enums} default strategy and per-type strategy annotations (via
 * {@link Qualifiers}), so the outcome tracks the registry it is obtained from. It is obtained per registry via
 * {@link #forRegistry(ConfigView)}; a forked registry resolves against its own {@code Enums} configuration.
 */
public final class EnumStrategyResolver {

    /**
     * Returns the enum-strategy resolver for the given registry, creating it on first use.
     *
     * @param config the configuration registry to resolve against
     * @return the registry's memoized enum-strategy resolver
     */
    public static EnumStrategyResolver forRegistry(final ConfigView config) {
        return config.readAs(EnumStrategyResolver.class, EnumStrategyResolver::new);
    }

    private final ConfigView registry;

    private EnumStrategyResolver(final ConfigView registry) {
        this.registry = registry;
    }

    /**
     * Determines which strategy is to be used for a given {@link QualifiedType}, falling back to
     * reading strategy annotations on the source class and/or using the configured default.
     *
     * @param <E> the {@link Enum} type
     * @param type qualified type to derive a strategy from
     * @return the strategy by which this enum should be handled
     */
    public <E extends Enum<E>> EnumStrategy findStrategy(final QualifiedType<E> type) {
        final Class<?> erasedType = getErasedType(type.getType());
        return JdbiOptionals.findFirstPresent(
            () -> doFindStrategy(type),
            () -> doFindStrategy(QualifiedType.of(erasedType).withAnnotations(registry.get(Qualifiers.class).findFor(erasedType)))
        ).orElseGet(() -> registry.get(Enums.class).getDefaultStrategy());
    }

    private static <T> Optional<EnumStrategy> doFindStrategy(final QualifiedType<T> type) {
        boolean hasByName = type.hasQualifier(EnumByName.class);
        boolean hasByOrdinal = type.hasQualifier(EnumByOrdinal.class);

        if (hasByName && hasByOrdinal) {
            throw new IllegalArgumentException(String.format(
                "%s is both %s and %s",
                type.getType(),
                EnumByName.class.getSimpleName(),
                EnumByOrdinal.class.getSimpleName()
            ));
        }

        if (hasByName) {
            return Optional.of(EnumStrategy.BY_NAME);
        } else if (hasByOrdinal) {
            return Optional.of(EnumStrategy.BY_ORDINAL);
        } else {
            return Optional.empty();
        }
    }
}

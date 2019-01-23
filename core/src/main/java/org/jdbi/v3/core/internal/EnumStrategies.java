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
package org.jdbi.v3.core.internal;

import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.enums.EnumByName;
import org.jdbi.v3.core.enums.EnumByOrdinal;
import org.jdbi.v3.core.enums.EnumStrategy;
import org.jdbi.v3.core.enums.Enums;
import org.jdbi.v3.core.qualifier.QualifiedType;

import static org.jdbi.v3.core.generic.GenericTypes.getErasedType;
import static org.jdbi.v3.core.qualifier.Qualifiers.getQualifiers;

public class EnumStrategies implements JdbiConfig<EnumStrategies> {
    private ConfigRegistry registry;

    public EnumStrategies() {}

    @Override
    public void setRegistry(ConfigRegistry registry) {
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
    public <E extends Enum<E>> EnumStrategy findStrategy(QualifiedType<E> type) {
        Class<?> erasedType = getErasedType(type.getType());
        return JdbiOptionals.findFirstPresent(
            () -> doFindStrategy(type),
            () -> doFindStrategy(QualifiedType.of(erasedType).withAnnotations(getQualifiers(erasedType)))
        ).orElseGet(() -> registry.get(Enums.class).getDefaultStrategy());
    }

    private static <T> Optional<EnumStrategy> doFindStrategy(QualifiedType<T> type) {
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

    @Override
    public EnumStrategies createCopy() {
        return new EnumStrategies();
    }
}

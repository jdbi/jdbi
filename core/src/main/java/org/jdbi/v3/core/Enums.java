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
package org.jdbi.v3.core;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

/**
 * Configuration for behavior related to {@link Enum}s.
 */
public class Enums implements JdbiConfig<Enums> {
    private EnumStrategy strategy;

    public Enums() {
        strategy = EnumStrategy.BY_NAME;
    }

    private Enums(Enums other) {
        strategy = other.strategy;
    }

    public EnumStrategy getDefaultStrategy() {
        return strategy;
    }

    public Enums defaultByName() {
        this.strategy = EnumStrategy.BY_NAME;
        return this;
    }

    public Enums defaultByOrdinal() {
        this.strategy = EnumStrategy.BY_ORDINAL;
        return this;
    }

    /**
     * Determines which strategy is to be used for a given {@link QualifiedType}, falling back to
     * reading strategy annotations on the source class and/or using the configured default.
     *
     * @param givenType qualified type to derive a strategy from
     * @param clazz the source class to read annotations from as a fallback
     * @param <E> the {@link Enum} type
     * @return the strategy by which this enum should be handled
     */
    public <E extends Enum<E>> EnumStrategy findStrategy(QualifiedType<E> givenType, Class<E> clazz) {
        return deriveStrategy(givenType)
            .orElseGet(() -> deriveStrategy(qualifyFromSource(clazz))
                .orElse(strategy));
    }

    private <E extends Enum<E>> Optional<EnumStrategy> deriveStrategy(QualifiedType<E> type) {
        boolean hasByName = type.hasQualifier(EnumByName.class);
        boolean hasByOrdinal = type.hasQualifier(EnumByOrdinal.class);

        if (hasByName && hasByOrdinal) {
            throw new IllegalArgumentException(String.format(
                "%s is both %s and %s",
                type,
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

    private <E extends Enum<E>> QualifiedType<E> qualifyFromSource(Class<E> type) {
        Set<? extends Annotation> onClass = Qualifiers.getQualifiers(type)
            .stream()
            .map(Annotation::annotationType)
            .map(AnnotationFactory::create)
            .collect(Collectors.toSet());

        return QualifiedType.of(type).with(onClass);
    }

    @Override
    public Enums createCopy() {
        return new Enums(this);
    }

    public enum EnumStrategy {
        BY_NAME, BY_ORDINAL
    }
}

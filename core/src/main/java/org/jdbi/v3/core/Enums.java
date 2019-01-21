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
    private EnumHandling handling;

    public Enums() {
        handling = EnumHandling.BY_NAME;
    }

    private Enums(Enums other) {
        handling = other.handling;
    }

    public EnumHandling getDefaultHandling() {
        return handling;
    }

    public Enums defaultByName() {
        this.handling = EnumHandling.BY_NAME;
        return this;
    }

    public Enums defaultByOrdinal() {
        this.handling = EnumHandling.BY_ORDINAL;
        return this;
    }

    public <E extends Enum<E>> EnumHandling findStrategy(QualifiedType<E> givenType, Class<E> clazz) {
        EnumHandling strategy = deriveStrategy(givenType);

        if (strategy == null) {
            strategy = deriveStrategy(qualifyFromSourceOrDefault(clazz));
        }

        return strategy;
    }

    private <E extends Enum<E>> EnumHandling deriveStrategy(QualifiedType<E> type) {
        boolean hasByName = type.getQualifiers().stream().anyMatch(EnumByName.class::isInstance);
        boolean hasByOrdinal = type.getQualifiers().stream().anyMatch(EnumByOrdinal.class::isInstance);

        if (hasByName && hasByOrdinal) {
            throw new IllegalArgumentException();
        }

        if (hasByName) {
            return EnumHandling.BY_NAME;
        } else if (hasByOrdinal) {
            return EnumHandling.BY_ORDINAL;
        } else {
            return null;
        }
    }

    private <E extends Enum<E>> QualifiedType<E> qualifyFromSourceOrDefault(Class<E> type) {
        Set<Class<? extends Annotation>> qualifiers = Qualifiers.getQualifiers(type)
            .stream()
            .map(Annotation::annotationType)
            .collect(Collectors.toSet());
        return qualifiers.isEmpty()
            ? QualifiedType.of(type).with(handling == EnumHandling.BY_NAME ? EnumByName.class : EnumByOrdinal.class)
            : QualifiedType.of(type).with(qualifiers.stream().map(AnnotationFactory::create).collect(Collectors.toSet()));
    }

    @Override
    public Enums createCopy() {
        return new Enums(this);
    }

    public enum EnumHandling {
        BY_NAME, BY_ORDINAL
    }
}

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
package org.jdbi.v3.core.internal.enums;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdbi.v3.core.EnumByName;
import org.jdbi.v3.core.EnumByOrdinal;
import org.jdbi.v3.core.Enums;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.generic.GenericTypes;
import org.jdbi.v3.core.internal.AnnotationFactory;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.qualifier.Qualifiers;

public class Unqualified implements ArgumentFactory, ColumnMapperFactory {
    private static final Unqualified INSTANCE = new Unqualified();

    public static Unqualified singleton() {
        return INSTANCE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        return EnumStrategy.getEnumType(type).flatMap(enumType -> config.get(ColumnMappers.class).findFor(onClassOrDefault(type, config)));
    }

    @Override
    public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
        return EnumStrategy.getEnumType(type).flatMap(enumType -> config.get(Arguments.class).findFor(onClassOrDefault(type, config), value));
    }

    // should be QT<?> but this causes type inference to fail on java8
    @SuppressWarnings("rawtypes")
    private QualifiedType onClassOrDefault(Type type, ConfigRegistry config) {
        Set<Class<? extends Annotation>> qualifiers = Qualifiers.getQualifiers(GenericTypes.getErasedType(type))
                .stream()
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());

        if (qualifiers.isEmpty()) {
            return QualifiedType.of(type).with(config.get(Enums.class).getDefaultQualifier());
        }

        if (qualifiers.contains(EnumByName.class) && qualifiers.contains(EnumByOrdinal.class)) {
            throw new IllegalArgumentException(String.format(
                "%s is both %s and %s",
                type,
                EnumByName.class.getSimpleName(),
                EnumByOrdinal.class.getSimpleName()
            ));
        }

        return QualifiedType.of(type).with(qualifiers.stream().map(AnnotationFactory::create).collect(Collectors.toSet()));
    }
}

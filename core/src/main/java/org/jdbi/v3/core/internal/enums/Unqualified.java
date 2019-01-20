package org.jdbi.v3.core.internal.enums;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
            qualifiers.add(config.get(Enums.class).getDefaultQualifier());
        }
        return QualifiedType.of(type).with(qualifiers.stream().map(AnnotationFactory::create).collect(Collectors.toSet()));
    }
}

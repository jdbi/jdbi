package org.jdbi.v3.sqlobject.config.internal;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.RegisterImmutableMappers;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.stream.Stream;

public class RegisterImmutableMappersImpl implements Configurer
{
    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        Configurer delegate = new RegisterImmutableMapperImpl();

        RegisterImmutableMappers registerImmutableMappers = (RegisterImmutableMappers) annotation;
        Stream.of(registerImmutableMappers.value()).forEach(anno -> delegate.configureForType(registry, anno, sqlObjectType));
    }

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        configureForType(registry, annotation, sqlObjectType);
    }
}

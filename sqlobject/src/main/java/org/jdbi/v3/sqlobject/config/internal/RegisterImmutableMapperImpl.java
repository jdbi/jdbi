package org.jdbi.v3.sqlobject.config.internal;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.mapper.reflect.ImmutableMapper;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.RegisterImmutableMapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class RegisterImmutableMapperImpl implements Configurer
{
    @Override
    public void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        RegisterImmutableMapper registerImmutableMapper = (RegisterImmutableMapper) annotation;
        Class<?> beanClass = registerImmutableMapper.value();
        String prefix = registerImmutableMapper.prefix();
        RowMappers mappers = registry.get(RowMappers.class);
        if (prefix.isEmpty()) {
            mappers.register(ImmutableMapper.factory(beanClass));
        }
        else {
            mappers.register(ImmutableMapper.factory(beanClass, prefix));
        }
    }

    @Override
    public void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        configureForType(registry, annotation, sqlObjectType);
    }
}

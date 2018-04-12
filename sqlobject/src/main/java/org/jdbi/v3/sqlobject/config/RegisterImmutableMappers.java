package org.jdbi.v3.sqlobject.config;

import org.jdbi.v3.sqlobject.config.internal.RegisterImmutableMappersImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterImmutableMappersImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterImmutableMappers {
    RegisterImmutableMapper[] value();
}

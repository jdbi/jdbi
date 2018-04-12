package org.jdbi.v3.sqlobject.config;

import org.jdbi.v3.sqlobject.config.internal.RegisterImmutableMapperImpl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@ConfiguringAnnotation(RegisterImmutableMapperImpl.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RegisterImmutableMappers.class)
public @interface RegisterImmutableMapper
{
    /**
     * The mapped immutable class.
     * @return the mapped immutable class.
     */
    Class<?> value();

    /**
     * Column name prefix for the immutable type. If omitted, defaults to no prefix.
     *
     * @return Column name prefix for the immutable type.
     */
    String prefix() default "";
}

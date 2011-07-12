package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GetGeneratedKeys
{
    Class<? extends ResultSetMapper> value() default FigureItOutResultSetMapper.class;
}

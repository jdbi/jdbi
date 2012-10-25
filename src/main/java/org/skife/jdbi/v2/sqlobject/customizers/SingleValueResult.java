package org.skife.jdbi.v2.sqlobject.customizers;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SingleValueResult
{
    Class<?> value() default Default.class;

    final class Default{}
}

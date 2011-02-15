package org.skife.jdbi.v2.unstable.eod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Bind
{
    String value();

    Class<? extends Binder> binder() default PrimitiveBindifier.class;
}

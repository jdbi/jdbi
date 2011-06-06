package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindBeanFactory.class)
public @interface BindBean
{
    String value() default "___jdbi_bare___";

}

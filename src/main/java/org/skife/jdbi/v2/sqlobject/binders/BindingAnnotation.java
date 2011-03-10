package org.skife.jdbi.v2.sqlobject.binders;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface BindingAnnotation
{
    Class<? extends BinderFactory> value();

}

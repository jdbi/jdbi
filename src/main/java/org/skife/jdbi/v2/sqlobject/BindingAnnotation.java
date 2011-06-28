package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark binding annotations. This allows for custom binding annotations.
 * Implementations must provide a {@link BinderFactory} class which will be used to create
 * Binders to do the binding
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface BindingAnnotation
{
    /**
     * Build a BinderFactory which will be used to build Binders, which will bind arguments
     */
    Class<? extends BinderFactory> value();

}

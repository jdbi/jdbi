package org.skife.jdbi.v2.sqlobject.customizers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to control the batch chunk size for sql batch operations.
 * If this annotation is present the value (or argument value if on
 * a parameter) will be used as the size for each batch statement to
 * execute.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
public @interface BatchChunkSize
{
    /**
     * The batch chunk size. Defaults to -1 which will raise an error, so
     * do not use the default. It is present for when the annotation is used
     * on a parameter, in which case this value will be ignored and the parameter value
     * will be used. The parameter type must be an int (or castable to an int).
     */
    int value() default -1;
}

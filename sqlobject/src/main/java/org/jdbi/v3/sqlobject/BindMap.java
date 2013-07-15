package org.jdbi.v3.sqlobject;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bind a {@code Map<String, Object>}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindMapFactory.class)
public @interface BindMap
{
    /**
     * The list of allowed map keys to bind.
     * If not specified, to binds all provided {@code Map} entries.  Any missing parameters will cause an exception.
     * If specified, binds all provided keys.  Missing entries are bound with a SQL {@code NULL}.
     */
    String[] value() default {};

    /**
     * If specified, key {@code key} will be bound as {@code prefix.key}.
     */
    String prefix() default BindBean.BARE_BINDING;

    /**
     * Specify key handling.
     * If false, {@code Map} keys must be strings, or an exception is thrown.
     * If true, any object may be the key, and it will be converted with {@link Object#toString()}.
     */
    boolean implicitKeyStringConversion() default false;
}

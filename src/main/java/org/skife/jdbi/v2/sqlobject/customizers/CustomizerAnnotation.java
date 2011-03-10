package org.skife.jdbi.v2.sqlobject.customizers;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate another annotation to mark is as an customizer annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface CustomizerAnnotation
{
    Class<? extends StatementCustomizerFactory> value();
}

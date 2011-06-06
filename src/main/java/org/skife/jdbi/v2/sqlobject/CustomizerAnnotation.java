package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate another annotation to mark is as an customizer annotation. A customizer annotation
 * allows for using {@link org.skife.jdbi.v2.tweak.StatementCustomizer} instances on sql object statements.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface CustomizerAnnotation
{
    /**
     * Specify the factory object which will create your statement customizer. The class
     * must have a no-arg constructor.
     */
    Class<? extends JDBCStatementCustomizerFactory> value();
}

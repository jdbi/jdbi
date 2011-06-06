package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to build customizing annotations. Use this to annotate an annotation. See examples
 * in the org.skife.jdbi.v2.sqlobject.customizers package.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlStatementCustomizingAnnotation
{
    /**
     * Specify a sql statement customizer factory which will be used to create
     * sql statement customizers.
     * @return a factory used to crate customizers for the customizing annotation
     */
    Class<? extends SqlStatementCustomizerFactory> value();
}

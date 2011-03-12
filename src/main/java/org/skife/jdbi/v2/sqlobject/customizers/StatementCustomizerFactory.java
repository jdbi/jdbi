package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;

/**
 * This interface is used in conjuntion with the {@link CustomizerAnnotation} to create
 * statement customizers. It will be instantiated each time it is needed, via a no-arg constructor.
 */
public interface StatementCustomizerFactory
{
    /**
     * This will be called when a customizer annotation is found on a method parameter.
     *
     * @param annotation the annotation instance which lead to this call.
     * @param arg The value for the argument at the time it is called.
     * @return a statement customizer which will be applied ot the generated sal statement
     */
    StatementCustomizer createForParameter(Annotation annotation, Object arg);

    /**
     * This will be called when a customizer annotation is found on a method.
     *
     * @param annotation the annotation which lead to this being called.
     * @return a statement customizer to apply to the statement being generated
     */
    StatementCustomizer createForMethod(Annotation annotation);
}

package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Interface used in conjunction with {@link SqlStatementCustomizingAnnotation} to generate
 * {@link SqlStatementCustomizer} instances.
 */
public interface SqlStatementCustomizerFactory
{
    /**
     * Used to create customizers for annotations on methods.
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @return the customizer which will be applied to the generated statement
     */
    public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method);

    /**
     * Used to create customizers for annotations on sql object interfaces
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @return the customizer which will be applied to the generated statement
     */
    public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType);

    /**
     * Used to create customizers for annotations on parameters
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @param arg the argument value for the annotated parameter
     * @return the customizer which will be applied to the generated statement
     */
    public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg);
}

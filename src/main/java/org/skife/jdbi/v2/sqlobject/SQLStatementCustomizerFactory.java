package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Interface used in conjunction with {@link SQLStatementCustomizingAnnotation} to generate
 * {@link SQLStatementCustomizer} instances.
 */
public interface SQLStatementCustomizerFactory
{
    /**
     * Used to create customizers for annotations on methods.
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @return the customizer which will be applied to the generated statement
     */
    public SQLStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method);

    /**
     * Used to create customizers for annotations on sql object interfaces
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @return the customizer which will be applied to the generated statement
     */
    public SQLStatementCustomizer createForType(Annotation annotation, Class sqlObjectType);

    /**
     * Used to create customizers for annotations on parameters
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @param arg the argument value for the annotated parameter
     * @return the customizer which will be applied to the generated statement
     */
    public SQLStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg);
}

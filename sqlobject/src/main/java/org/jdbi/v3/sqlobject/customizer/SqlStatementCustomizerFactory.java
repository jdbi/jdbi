/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.sqlobject.customizer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Interface used in conjunction with {@link SqlStatementCustomizingAnnotation} to generate
 * {@link SqlStatementCustomizer} instances.
 */
public interface SqlStatementCustomizerFactory
{
    /**
     * Used to create customizers for annotations on sql object interfaces
     *
     * @param registry the configuration registry
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @return the customizer which will be applied to the generated statement
     */
    default SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
    {
        throw new UnsupportedOperationException("Not supported for type");
    }

    /**
     * Used to create customizers for annotations on methods.
     *
     * @param registry the configuration registry
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @return the customizer which will be applied to the generated statement
     */
    default SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
    {
        throw new UnsupportedOperationException("Not supported for method");
    }

    /**
     * Used to create customizers for annotations on parameters
     *
     * @param registry the configuration registry
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @param param the parameter which was annotated
     * @param index the method parameter index
     * @param arg the argument value for the annotated parameter
     * @return the customizer which will be applied to the generated statement
     */
    default SqlStatementParameterCustomizer createForParameter(ConfigRegistry registry,
                                                      Annotation annotation,
                                                      Class<?> sqlObjectType,
                                                      Method method,
                                                      Parameter param,
                                                      int index)
    {
        throw new UnsupportedOperationException("Not supported for parameter");
    }

    /**
     * Empty SqlStatementCustomizer.  Useful for implementations that
     * change the configuration {@code registry} but do not do
     * further per-statement customizations.
     */
    static SqlStatementCustomizer NONE = s -> {};
}

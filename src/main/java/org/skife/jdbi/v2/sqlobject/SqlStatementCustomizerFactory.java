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
    SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method);

    /**
     * Used to create customizers for annotations on sql object interfaces
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @return the customizer which will be applied to the generated statement
     */
    SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType);

    /**
     * Used to create customizers for annotations on parameters
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @param arg the argument value for the annotated parameter
     * @return the customizer which will be applied to the generated statement
     */
    SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg);
}

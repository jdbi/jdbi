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
import java.lang.reflect.Type;

/**
 * Interface used in conjunction with {@link SqlStatementCustomizingAnnotation} to generate
 * {@link SqlStatementCustomizer} instances.
 */
public interface SqlStatementCustomizerFactory {
    /**
     * Used to create customizers for annotations on sql object interfaces
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @return the customizer which will be applied to the generated statement
     */
    default SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
        throw new UnsupportedOperationException("Not supported for type");
    }

    /**
     * Used to create customizers for annotations on methods.
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @return the customizer which will be applied to the generated statement
     */
    default SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        throw new UnsupportedOperationException("Not supported for method");
    }

    /**
     * Used to create customizers for annotations on parameters
     *
     * @param annotation the annotation which lead to the method being called
     * @param sqlObjectType sql object class (interface)
     * @param method the method which was annotated
     * @param param the parameter which was annotated
     * @param index the method parameter index
     * @param paramType the type of the parameter
     * @return the customizer which will be applied to the generated statement
     */
    default SqlStatementParameterCustomizer createForParameter(Annotation annotation,
                                                               Class<?> sqlObjectType,
                                                               Method method,
                                                               Parameter param,
                                                               int index,
                                                               Type paramType) {
        throw new UnsupportedOperationException("Not supported for parameter");
    }
}

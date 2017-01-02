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
package org.jdbi.v3.sqlobject.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Configures {@link ConfigRegistry} instances to satisfy the contract of a
 * {@link ConfiguringAnnotation}-annotated annotation.
 */
public interface Configurer {
    /**
     * Configures the registry for the given annotation on a sql object type.
     *
     * @param registry      the registry to configure
     * @param annotation    the annotation
     * @param sqlObjectType the sql object type which was annotated
     */
    default void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType) {
        throw new UnsupportedOperationException("Not supported for type");
    }

    /**
     * Configures the registry for the given annotation on a sql object method.
     *
     * @param registry      the registry to configure
     * @param annotation    the annotation
     * @param sqlObjectType the sql object type
     * @param method        the method which was annotated
     */
    default void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method) {
        throw new UnsupportedOperationException("Not supported for method");
    }
}

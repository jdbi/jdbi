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
package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.jdbi.v3.core.config.ConfigRegistry;

/**
 * Generates {@link Consumer}&lt;{@link ConfigRegistry}&gt; instances to satisfy the contract of a
 * {@link ConfiguringAnnotation}-annotated annotation.
 */
public interface ConfigurerFactory {
    /**
     * Create a configurer for the given annotation on a sql object type.
     *
     * @param annotation    the annotation
     * @param sqlObjectType the sql object type which was annotated
     * @return a configurer which will be applied to the {@link ConfigRegistry}.
     */
    default Consumer<ConfigRegistry> createForType(Annotation annotation, Class<?> sqlObjectType) {
        throw new UnsupportedOperationException("Not supported for type");
    }

    /**
     * Create a configurer for the given annotation on a sql object method.
     *
     * @param annotation    the annotation
     * @param sqlObjectType the sql object type
     * @param method        the method which was annotated
     * @return a configurer which will be applied to the {@link ConfigRegistry}.
     */
    default Consumer<ConfigRegistry> createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method) {
        throw new UnsupportedOperationException("Not supported for method");
    }
}

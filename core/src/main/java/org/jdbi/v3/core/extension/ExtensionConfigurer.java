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
package org.jdbi.v3.core.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.meta.Alpha;

/**
 * Configures {@link ConfigRegistry} instances. Implementation classes
 * are referenced from annotations that are marked with the {@link org.jdbi.v3.core.extension.annotation.UseExtensionConfigurer} annotation.
 * <br>
 * Instances of this interface update the configuration used to execute a specific method e.g. to
 * register row mapper needed for the execution of the underlying ExtensionHandler.
 *
 * @since 3.38.0
 */
@Alpha
public interface ExtensionConfigurer {
    /**
     * Updates configuration for the given annotation on an extension type.
     *
     * @param registry      the registry to configure
     * @param annotation    the annotation
     * @param extensionType the extension type which was annotated
     */
    default void configureForType(ConfigRegistry registry, Annotation annotation, Class<?> extensionType) {
        throw new UnsupportedOperationException("Not supported for type");
    }

    /**
     * Configures the registry for the given annotation on a extension type method.
     *
     * @param registry      the registry to configure
     * @param annotation    the annotation
     * @param extensionType the extension type
     * @param method        the method which was annotated
     */
    default void configureForMethod(ConfigRegistry registry, Annotation annotation, Class<?> extensionType, Method method) {
        throw new UnsupportedOperationException("Not supported for method");
    }

}

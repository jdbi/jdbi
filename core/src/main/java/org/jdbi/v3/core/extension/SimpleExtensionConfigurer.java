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
 * Configurer base class that applies the same configuration changes independent whether an annotation
 * is placed on the type or a method.
 */
@Alpha
public abstract class SimpleExtensionConfigurer implements ExtensionConfigurer {

    @Override
    public final void configureForType(ConfigRegistry config, Annotation annotation, Class<?> extensionType) {
        configure(config, annotation, extensionType);
    }

    @Override
    public final void configureForMethod(ConfigRegistry config, Annotation annotation, Class<?> extensionType, Method method) {
        configure(config, annotation, extensionType);
    }

    /**
     * Updates configuration for the given annotation on an extension type.
     *
     * @param config        The {@link ConfigRegistry} object to configure
     * @param annotation    The annotation that invoked this method
     * @param extensionType the extension type which was annotated
     */
    public abstract void configure(ConfigRegistry config, Annotation annotation, Class<?> extensionType);
}

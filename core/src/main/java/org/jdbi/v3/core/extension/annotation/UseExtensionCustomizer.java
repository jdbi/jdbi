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
package org.jdbi.v3.core.extension.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.v3.meta.Alpha;

/**
 * Meta-Annotation used to identify extension method decorating annotations. Use this to annotate an annotation.
 *
 * @since 3.38.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Alpha
public @interface UseExtensionCustomizer {
    /**
     * {@link ExtensionHandlerCustomizer} class that decorates {@link ExtensionHandler} instances for methods annotated with the associated annotation.
     * <br>
     * The extension customizer must have either a public no-arguments, a {@code (Method method)}, or
     * a {@code (Class<?> extensionType, Method method)} constructor. If the constructor takes one or more
     * arguments, it will get the extension type and the invoked method passed in at construction time.
     *
     * @return the {@link ExtensionHandlerCustomizer} class
     */
    Class<? extends ExtensionHandlerCustomizer> value();
}

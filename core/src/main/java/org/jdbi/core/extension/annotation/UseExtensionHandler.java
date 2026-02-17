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
package org.jdbi.core.extension.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.ExtensionHandler;
import org.jdbi.meta.Alpha;

/**
 * Meta-Annotation used to map a method to an extension handler. Use this to annotate an annotation.
 *
 * @since 3.38.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Alpha
public @interface UseExtensionHandler {

    /**
     * {@link ExtensionHandler} factory annotation that creates the extension handler for the decorated method.
     * <br>
     * The extension handler must have either a public no-arguments, a {@code (Method method)}, or
     * a {@code (Class<?> extensionType, Method method)} constructor. If the constructor takes one or more
     * arguments, it will get the extension type and the invoked method passed in at construction time.
     *
     * @return the {@link ExtensionHandler} class
     */
    Class<? extends ExtensionHandler> value();

    /**
     * An extension must declare an id and tag all annotations with it. This allows the {@link ExtensionFactory} to decide
     * whether it will process a class or not by looking at the annotations on the class and method and determine whether these
     * have the right id for the extension.
     *
     * @return A string with the extension identifier
     */
    String id();
}

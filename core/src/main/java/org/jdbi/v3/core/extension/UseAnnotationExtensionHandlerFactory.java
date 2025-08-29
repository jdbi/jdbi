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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.internal.JdbiClassUtils;

import static java.lang.String.format;

/**
 * Processes {@link UseExtensionHandler} annotations on methods.
 */
final class UseAnnotationExtensionHandlerFactory implements ExtensionHandlerFactory {

    private static final Class<?>[] EXTENSION_HANDLER_TYPES = {Class.class, Method.class};

    static final ExtensionHandlerFactory INSTANCE = new UseAnnotationExtensionHandlerFactory();

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {
        return !method.isBridge() && !findAnnotations(method).isEmpty();
    }

    private static boolean matchAnnotation(Annotation a) {
        return a.annotationType().isAnnotationPresent(UseExtensionHandler.class);
    }

    static List<Class<?>> findAnnotations(Method method) {
        return Stream.of(method.getAnnotations())
                .filter(UseAnnotationExtensionHandlerFactory::matchAnnotation)
                .map(Annotation::annotationType)
                .collect(Collectors.toList());
    }


    @Override
    public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {

        List<Class<?>> extensionAnnotations = findAnnotations(method);

        if (extensionAnnotations.size() > 1) {
            throw new IllegalStateException(
                    format("Mutually exclusive extension annotations on method %s.%s: %s",
                            extensionType.getName(),
                            method.getName(),
                            extensionAnnotations));
        }

        if (method.isDefault() && !method.isSynthetic()) {
            throw new IllegalStateException(format(
                    "Default method %s.%s has @%s annotation. "
                            + "Extension type methods may be default, or have a @UseExtensionHandler annotation, but not both.",
                    extensionType.getSimpleName(),
                    method.getName(),
                    extensionAnnotations.get(0).getSimpleName()));
        }

        return extensionAnnotations.stream()
                .map(type -> type.getAnnotation(UseExtensionHandler.class))
                .map(UseExtensionHandler::value)
                .map(klass -> (ExtensionHandler) JdbiClassUtils.findConstructorAndCreateInstance(klass, EXTENSION_HANDLER_TYPES,
                        handle -> handle.invokeExact(extensionType, method)))
                .findFirst();
    }
}

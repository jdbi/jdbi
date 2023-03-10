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

import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.CheckedCallable;

import static java.lang.String.format;

/**
 * Processes {@link UseExtensionHandler} annotations on methods.
 */
final class UseExtensionAnnotationHandlerFactory implements ExtensionHandlerFactory {

    static final ExtensionHandlerFactory FACTORY = new UseExtensionAnnotationHandlerFactory();

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {

        if (method.isBridge()) {
            return false;
        }

        return !findAnnotations(method).isEmpty();
    }

    private static boolean matchAnnotation(Annotation a) {
        return a.annotationType().isAnnotationPresent(UseExtensionHandler.class);
    }

    static List<Class<?>> findAnnotations(Method method) {
        return Stream.of(method.getAnnotations())
                .filter(UseExtensionAnnotationHandlerFactory::matchAnnotation)
                .map(Annotation::annotationType)
                .collect(Collectors.toList());
    }


    @Override
    public Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method) {

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
                .map(a -> createExtensionHandler(a.value(), extensionType, method))
                .findFirst();
    }

    private ExtensionHandler createExtensionHandler(Class<? extends ExtensionHandler> handlerType, Class<?> extensionObjectType, Method method) {

        CheckedCallable[] callables = {
                () -> handlerType.getConstructor(Class.class, Method.class).newInstance(extensionObjectType, method),
                () -> handlerType.getConstructor(Method.class).newInstance(method),
                () -> handlerType.getConstructor().newInstance()
        };

        for (CheckedCallable<ExtensionHandler> callable : callables) {
            Optional<ExtensionHandler> handler = JdbiClassUtils.createInstanceIfPossible(callable);
            if (handler.isPresent()) {
                return handler.get();
            }
        }

        throw new IllegalStateException(format("ExtensionHandler class %s cannot be instantiated. "
                + "Expected a constructor with parameters (Class, Method), (Method), or ().", handlerType));
    }
}

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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.jdbi.v3.core.extension.annotation.ExtensionCustomizationOrder;
import org.jdbi.v3.core.extension.annotation.UseExtensionCustomizer;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.CheckedCallable;

import static java.lang.String.format;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Applies decorations to method handlers, according to any {@link UseExtensionCustomizer} decorating annotations
 * present on the method. If multiple decorating annotations are present, the order of application can be controlled
 * using the {@link ExtensionCustomizationOrder} annotation.
 */
final class UseExtensionAnnotationHandlerCustomizer implements ExtensionHandlerCustomizer {

    static final ExtensionHandlerCustomizer HANDLER = new UseExtensionAnnotationHandlerCustomizer();

    @Override
    public ExtensionHandler customize(ExtensionHandler delegate, Class<?> extensionType, Method method) {
        ExtensionHandler extensionHandler = delegate;

        List<Class<? extends Annotation>> annotationTypes = Stream.of(method, extensionType)
                .map(AnnotatedElement::getAnnotations)
                .flatMap(Arrays::stream)
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(UseExtensionCustomizer.class))
                .collect(toCollection(ArrayList::new));

        Stream.of(method, extensionType)
                .map(e -> e.getAnnotation(ExtensionCustomizationOrder.class))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(order -> annotationTypes.sort(createComparator(order).reversed()));

        List<ExtensionHandlerCustomizer> customizers = annotationTypes.stream()
                .map(type -> type.getAnnotation(UseExtensionCustomizer.class))
                .map(a -> createCustomizer(a.value(), extensionType, method))
                .collect(toList());

        for (ExtensionHandlerCustomizer customizer : customizers) {
            extensionHandler = customizer.customize(extensionHandler, extensionType, method);
        }

        return extensionHandler;
    }

    private Comparator<Class<? extends Annotation>> createComparator(ExtensionCustomizationOrder order) {
        List<Class<? extends Annotation>> ordering = Arrays.asList(order.value());

        return Comparator.comparingInt(type -> {
            int index = ordering.indexOf(type);
            return index == -1 ? ordering.size() : index;
        });
    }

    private static ExtensionHandlerCustomizer createCustomizer(Class<? extends ExtensionHandlerCustomizer> customizerType,
            Class<?> extensionObjectType, Method method) {

        CheckedCallable[] callables = {
                () -> customizerType.getConstructor().newInstance(),
                () -> customizerType.getConstructor(Method.class).newInstance(method),
                () -> customizerType.getConstructor(Class.class, Method.class).newInstance(extensionObjectType, method)
        };

        for (CheckedCallable<ExtensionHandlerCustomizer> callable : callables) {
            Optional<ExtensionHandlerCustomizer> handler = JdbiClassUtils.createInstanceIfPossible(callable);
            if (handler.isPresent()) {
                return handler.get();
            }
        }

        throw new IllegalStateException(format("ExtensionHandlerCustomizer class %s cannot be instantiated. "
                + "Expected a constructor with parameters (Class, Method), (Method), or ().", customizerType));
    }
}

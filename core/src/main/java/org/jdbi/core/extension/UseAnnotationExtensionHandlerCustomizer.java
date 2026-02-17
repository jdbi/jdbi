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
package org.jdbi.core.extension;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.jdbi.core.extension.annotation.ExtensionHandlerCustomizationOrder;
import org.jdbi.core.extension.annotation.UseExtensionHandlerCustomizer;
import org.jdbi.core.internal.JdbiClassUtils;

import static java.util.stream.Collectors.toCollection;

/**
 * Applies decorations to method handlers, according to any {@link UseExtensionHandlerCustomizer} decorating annotations
 * present on the method. If multiple decorating annotations are present, the order of application can be controlled
 * using the {@link ExtensionHandlerCustomizationOrder} annotation.
 */
final class UseAnnotationExtensionHandlerCustomizer implements ExtensionHandlerCustomizer {

    private static final Class<?>[] EXTENSION_HANDLER_CUSTOMIZER_TYPES = {Class.class, Method.class};

    static final ExtensionHandlerCustomizer INSTANCE = new UseAnnotationExtensionHandlerCustomizer();

    @Override
    public ExtensionHandler customize(ExtensionHandler delegate, Class<?> extensionType, Method method) {
        ExtensionHandler extensionHandler = delegate;

        List<Class<? extends Annotation>> annotationTypes = Stream.of(method, extensionType)
                .map(AnnotatedElement::getAnnotations)
                .flatMap(Arrays::stream)
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(UseExtensionHandlerCustomizer.class))
                .collect(toCollection(ArrayList::new));

        Stream.of(method, extensionType)
                .map(e -> e.getAnnotation(ExtensionHandlerCustomizationOrder.class))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(order -> annotationTypes.sort(createComparator(order).reversed()));

        List<? extends ExtensionHandlerCustomizer> customizers = annotationTypes.stream()
                .map(type -> type.getAnnotation(UseExtensionHandlerCustomizer.class))
                .map(UseExtensionHandlerCustomizer::value)
                .map(klass -> JdbiClassUtils.findConstructorAndCreateInstance(klass, EXTENSION_HANDLER_CUSTOMIZER_TYPES,
                        handle -> handle.invokeExact(extensionType, method)))
                .toList();

        for (ExtensionHandlerCustomizer customizer : customizers) {
            extensionHandler = customizer.customize(extensionHandler, extensionType, method);
        }

        return extensionHandler;
    }

    private Comparator<Class<? extends Annotation>> createComparator(ExtensionHandlerCustomizationOrder order) {
        List<Class<? extends Annotation>> ordering = Arrays.asList(order.value());

        return Comparator.comparingInt(type -> {
            int index = ordering.indexOf(type);
            return index == -1 ? ordering.size() : index;
        });
    }
}

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Applies decorations to method handlers, according to any {@link SqlMethodDecoratingAnnotation decorating annotations}
 * present on the method. If multiple decorating annotations are present, the order of application can be controlled
 * using the {@link DecoratorOrder} annotation.
 * <p>
 * This decorator is registered out of the box.
 * </p>
 */
class SqlMethodAnnotatedHandlerDecorator implements HandlerDecorator {
    @Override
    public Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method) {
        Handler handler = base;

        List<Class<? extends Annotation>> annotationTypes = Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(SqlMethodDecoratingAnnotation.class))
                .collect(toList());

        Stream.of(method, sqlObjectType)
                .map(e -> e.getAnnotation(DecoratorOrder.class))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(order -> annotationTypes.sort(createDecoratorComparator(order).reversed()));

        List<HandlerDecorator> decorators = annotationTypes.stream()
                .map(type -> type.getAnnotation(SqlMethodDecoratingAnnotation.class))
                .map(a -> buildDecorator(a.value()))
                .collect(toList());

        for (HandlerDecorator decorator : decorators) {
            handler = decorator.decorateHandler(handler, sqlObjectType, method);
        }

        return handler;
    }

    private Comparator<Class<? extends Annotation>> createDecoratorComparator(DecoratorOrder order) {
        List<Class<? extends Annotation>> ordering = Arrays.asList(order.value());

        return Comparator.comparingInt(type -> {
            int index = ordering.indexOf(type);
            return index == -1 ? ordering.size() : index;
        });
    }

    private static HandlerDecorator buildDecorator(Class<? extends HandlerDecorator> decoratorClass) {
        try {
            return decoratorClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Decorator class " + decoratorClass + "cannot be instantiated", e);
        }
    }

}

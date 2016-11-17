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

import static java.util.Collections.synchronizedMap;
import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.jdbi.v3.core.ConfigRegistry;
import org.jdbi.v3.core.ExtensionMethod;
import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.mixins.Transactional;

public class SqlObjectFactory implements ExtensionFactory {
    private static final Object[] NO_ARGS = new Object[0];

    private final Map<Method, Handler> mixinHandlers = new HashMap<>();
    private final Map<Class<?>, Map<Method, Handler>> handlersCache = synchronizedMap(new WeakHashMap<>());
    private final Map<Class<? extends ConfigurerFactory>, ConfigurerFactory>
            configurerFactories = synchronizedMap(new WeakHashMap<>());

    SqlObjectFactory() {
        mixinHandlers.putAll(TransactionalHelper.handlers());
        mixinHandlers.putAll(GetHandleHelper.handlers());
    }

    @Override
    public boolean accepts(Class<?> extensionType) {
        if (!extensionType.isInterface()) {
            throw new IllegalArgumentException("SQL Objects are only supported for interfaces.");
        }

        if (!Modifier.isPublic(extensionType.getModifiers())) {
            throw new IllegalArgumentException("SQL Object types must be public.");
        }

        if (GetHandle.class.isAssignableFrom(extensionType) ||
                Transactional.class.isAssignableFrom(extensionType)) {
            return true;
        }

        return Stream.of(extensionType.getMethods())
                .flatMap(m -> Stream.of(m.getAnnotations()))
                .anyMatch(a -> a.annotationType().isAnnotationPresent(SqlMethodAnnotation.class));
    }

    /**
     * Create a sql object of the specified type bound to this handle. Any state changes to the handle, or the sql
     * object, such as transaction status, closing it, etc, will apply to both the object and the handle.
     *
     * @param extensionType the type of sql object to create
     * @param handle the Handle instance to attach ths sql object to
     * @return the new sql object bound to this handle
     */
    @Override
    public <E> E attach(Class<E> extensionType, HandleSupplier handle) {
        ConfigRegistry instanceConfig = handle.getConfig().createChild();
        Map<Method, Handler> handlers = methodHandlersFor(extensionType);

        forEachConfigurerFactory(extensionType, (factory, annotation) ->
                factory.createForType(annotation, extensionType).accept(instanceConfig));

        InvocationHandler invocationHandler = createInvocationHandler(extensionType, handlers, instanceConfig, handle);
        return extensionType.cast(
                Proxy.newProxyInstance(
                        extensionType.getClassLoader(),
                        new Class[]{extensionType},
                        invocationHandler));
    }

    private Map<Method, Handler> methodHandlersFor(Class<?> sqlObjectType) {
        return handlersCache.computeIfAbsent(sqlObjectType, type -> {
            final Map<Method, Handler> handlers = new HashMap<>();

            handlers.putAll(EqualsHandler.handler());
            handlers.putAll(ToStringHandler.handler(sqlObjectType.getName()));
            handlers.putAll(HashCodeHandler.handler());
            handlers.putAll(FinalizeHandler.handlerFor(sqlObjectType));

            for (Method method : sqlObjectType.getMethods()) {
                handlers.computeIfAbsent(method, m -> buildMethodHandler(sqlObjectType, m));
            }

            return handlers;
        });
    }

    private Handler buildMethodHandler(Class<?> sqlObjectType, Method method) {
        if (mixinHandlers.containsKey(method)) {
            return mixinHandlers.get(method);
        }

        Handler handler = buildBaseHandler(sqlObjectType, method);
        return addDecorators(handler, sqlObjectType, method);
    }

    private Handler buildBaseHandler(Class<?> sqlObjectType, Method method) {
        List<Class<?>> sqlMethodAnnotations = Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(SqlMethodAnnotation.class))
                .collect(toList());

        if (sqlMethodAnnotations.size() > 1) {
            throw new IllegalStateException(
                    String.format("Mutually exclusive annotations on method %s.%s: %s",
                            sqlObjectType.getName(),
                            method.getName(),
                            sqlMethodAnnotations));
        }

        if (method.isDefault()) {
            if (!sqlMethodAnnotations.isEmpty()) {
                throw new IllegalStateException(String.format(
                        "Default method %s.%s has @%s annotation. " +
                                "SQL object methods may be default, or have a SQL method annotation, but not both.",
                        sqlObjectType.getSimpleName(),
                        method.getName(),
                        sqlMethodAnnotations.get(0).getSimpleName()));
            }

            Stream.of(method.getAnnotations()).map(Annotation::annotationType)
                    .filter(type -> type.isAnnotationPresent(SqlStatementCustomizingAnnotation.class))
                    .findFirst()
                    .ifPresent(type -> {
                        throw new IllegalStateException(String.format(
                                "Default method %s.%s has @%s annotation. Statement customizing annotations don't " +
                                        "work on default methods.",
                                sqlObjectType.getSimpleName(),
                                method.getName(),
                                type.getSimpleName()));
                    });

            for (Parameter parameter : method.getParameters()) {
                Stream.of(parameter.getAnnotations())
                        .map(Annotation::annotationType)
                        .filter(type -> type.isAnnotationPresent(SqlStatementCustomizingAnnotation.class))
                        .findFirst()
                        .ifPresent(type -> {
                            throw new IllegalStateException(String.format(
                                    "Default method %s.%s parameter %s has @%s annotation. Statement customizing " +
                                            "annotations don't work on default methods.",
                                    sqlObjectType.getSimpleName(),
                                    parameter.getName(),
                                    method.getName(),
                                    type.getSimpleName()));
                        });

                Stream.of(parameter.getAnnotations())
                        .map(Annotation::annotationType)
                        .filter(type -> type.isAnnotationPresent(BindingAnnotation.class))
                        .findFirst()
                        .ifPresent(type -> {
                            throw new IllegalStateException(String.format(
                                    "Default method %s.%s parameter %s has @%s annotation. Binding annotations " +
                                            "don't work on default methods.",
                                    sqlObjectType.getSimpleName(),
                                    parameter.getName(),
                                    method.getName(),
                                    type.getSimpleName()));
                        });
            }

            return new DefaultMethodHandler(method);
        }

        return sqlMethodAnnotations.stream()
                .map(type -> type.getAnnotation(SqlMethodAnnotation.class))
                .map(a -> buildFactory(a.value()))
                .map(factory -> factory.buildHandler(sqlObjectType, method))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "Method %s.%s must be default or be annotated with a SQL method annotation.",
                        sqlObjectType.getSimpleName(),
                        method.getName())));
    }

    private Handler addDecorators(Handler handler, Class<?> sqlObjectType, Method method) {
        List<Class<? extends Annotation>> annotationTypes = Stream.of(method.getAnnotations())
                .map(Annotation::annotationType)
                .filter(type -> type.isAnnotationPresent(SqlMethodDecoratingAnnotation.class))
                .collect(toList());

        Stream.of(method, sqlObjectType)
                .map(e -> e.getAnnotation(DecoratorOrder.class))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(order -> {
                    annotationTypes.sort(createDecoratorComparator(order).reversed());
                });

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

        ToIntFunction<Class<? extends Annotation>> indexOf = type -> {
            int index = ordering.indexOf(type);
            return index == -1 ? ordering.size() : index;
        };

        return (l, r) -> indexOf.applyAsInt(l) - indexOf.applyAsInt(r);
    }

    private static HandlerFactory buildFactory(Class<? extends HandlerFactory> factoryClazz) {
        HandlerFactory factory;
        try {
            factory = factoryClazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Factory class " + factoryClazz + "cannot be instantiated", e);
        }
        return factory;
    }

    private static HandlerDecorator buildDecorator(Class<? extends HandlerDecorator> decoratorClass) {
        HandlerDecorator decorator;
        try {
            decorator = decoratorClass.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Decorator class " + decoratorClass + "cannot be instantiated", e);
        }
        return decorator;
    }

    private InvocationHandler createInvocationHandler(Class<?> sqlObjectType,
                                                      Map<Method, Handler> handlers,
                                                      ConfigRegistry instanceConfig,
                                                      HandleSupplier handle) {
        return (proxy, method, args) -> {
            Handler handler = handlers.get(method);

            ConfigRegistry methodConfig = instanceConfig.createChild();
            forEachConfigurerFactory(method, (factory, annotation) ->
                    factory.createForMethod(annotation, sqlObjectType, method).accept(methodConfig));

            return handle.invokeInContext(new ExtensionMethod(sqlObjectType, method), methodConfig,
                    () -> handler.invoke(proxy, method, args == null ? NO_ARGS : args, handle));
        };
    }

    private void forEachConfigurerFactory(AnnotatedElement element, BiConsumer<ConfigurerFactory, Annotation> consumer) {
        Stream.of(element.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(ConfiguringAnnotation.class))
                .forEach(a -> {
                    ConfiguringAnnotation meta = a.annotationType()
                            .getAnnotation(ConfiguringAnnotation.class);

                    consumer.accept(getConfigurerFactory(meta.value()), a);
                });
    }

    private ConfigurerFactory getConfigurerFactory(Class<? extends ConfigurerFactory> factoryClass) {
        return configurerFactories.computeIfAbsent(factoryClass, c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to instantiate configurer factory class " + factoryClass, e);
            }
        });
    }
}

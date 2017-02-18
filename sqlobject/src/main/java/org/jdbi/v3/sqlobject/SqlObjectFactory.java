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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

public class SqlObjectFactory implements ExtensionFactory {
    private static final Object[] NO_ARGS = new Object[0];

    private final Map<Class<?>, Map<Method, Handler>> handlersCache = synchronizedMap(new WeakHashMap<>());
    private final Map<Class<? extends Configurer>, Configurer> configurers = synchronizedMap(new WeakHashMap<>());

    SqlObjectFactory() { }

    @Override
    public boolean accepts(Class<?> extensionType) {
        if (!extensionType.isInterface()) {
            throw new IllegalArgumentException("SQL Objects are only supported for interfaces.");
        }

        if (!Modifier.isPublic(extensionType.getModifiers())) {
            throw new IllegalArgumentException("SQL Object types must be public.");
        }

        if (SqlObject.class.isAssignableFrom(extensionType)) {
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

        ConfigRegistry instanceConfig = handle.getConfig().createCopy();
        forEachConfigurer(extensionType, (configurer, annotation) ->
                configurer.configureForType(instanceConfig, annotation, extensionType));

        DefaultHandlerFactory defaultHandlerFactory = instanceConfig
                .get(SqlObjects.class)
                .getDefaultHandlerFactory();

        Map<Method, Handler> handlers = methodHandlersFor(extensionType, defaultHandlerFactory);
        InvocationHandler invocationHandler = createInvocationHandler(extensionType, instanceConfig, handlers, handle);
        return extensionType.cast(
                Proxy.newProxyInstance(
                        extensionType.getClassLoader(),
                        new Class[]{extensionType},
                        invocationHandler));
    }

    private Map<Method, Handler> methodHandlersFor(Class<?> sqlObjectType, DefaultHandlerFactory defaultHandlerFactory) {
        return handlersCache.computeIfAbsent(sqlObjectType, type -> {
            final Map<Method, Handler> handlers = new HashMap<>();

            handlers.putAll(handlerEntry((t, a, h) ->
                    sqlObjectType.getName() + '@' + Integer.toHexString(t.hashCode()),
                Object.class, "toString"));
            handlers.putAll(handlerEntry((t, a, h) -> t == a[0], Object.class, "equals", Object.class));
            handlers.putAll(handlerEntry((t, a, h) -> System.identityHashCode(t), Object.class, "hashCode"));
            handlers.putAll(handlerEntry((t, a, h) -> h.getHandle(), SqlObject.class, "getHandle"));
            try {
                handlers.putAll(handlerEntry((t, a, h) -> null, sqlObjectType, "finalize"));
            } catch (IllegalStateException expected) { } // optional implementation

            for (Method method : sqlObjectType.getMethods()) {
                handlers.computeIfAbsent(method, m -> buildMethodHandler(sqlObjectType, m, defaultHandlerFactory));
            }

            return handlers;
        });
    }

    private Handler buildMethodHandler(Class<?> sqlObjectType, Method method, DefaultHandlerFactory defaultHandlerFactory) {
        Handler handler = buildBaseHandler(sqlObjectType, method, defaultHandlerFactory);
        return addDecorators(handler, sqlObjectType, method);
    }

    private Handler buildBaseHandler(Class<?> sqlObjectType, Method method, DefaultHandlerFactory defaultHandlerFactory) {
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

        Optional<Handler> maybeHandler = defaultHandlerFactory.build(sqlObjectType, method);
        if (maybeHandler.isPresent()) {
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
                                    method.getName(),
                                    parameter.getName(),
                                    type.getSimpleName()));
                        });
            }

            return maybeHandler.get();
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

    private static Map<Method, Handler> handlerEntry(Handler handler, Class<?> klass, String methodName, Class<?>... parameterTypes) {
        try {
            return Collections.singletonMap(klass.getMethod(methodName, parameterTypes), handler);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(
                    String.format("can't find %s#%s%s", klass.getName(), methodName, Arrays.asList(parameterTypes)), e);
        }
    }

    private InvocationHandler createInvocationHandler(Class<?> sqlObjectType,
                                                      ConfigRegistry instanceConfig,
                                                      Map<Method, Handler> handlers,
                                                      HandleSupplier handle) {
        return (proxy, method, args) -> {
            Handler handler = handlers.get(method);

            ConfigRegistry methodConfig = instanceConfig.createCopy();
            forEachConfigurer(method, (configurer, annotation) ->
                    configurer.configureForMethod(methodConfig, annotation, sqlObjectType, method));

            return handle.invokeInContext(new ExtensionMethod(sqlObjectType, method), methodConfig,
                    () -> handler.invoke(proxy, args == null ? NO_ARGS : args, handle));
        };
    }

    private void forEachConfigurer(AnnotatedElement element, BiConsumer<Configurer, Annotation> consumer) {
        Stream.of(element.getAnnotations())
                .filter(a -> a.annotationType().isAnnotationPresent(ConfiguringAnnotation.class))
                .forEach(a -> {
                    ConfiguringAnnotation meta = a.annotationType()
                            .getAnnotation(ConfiguringAnnotation.class);

                    consumer.accept(getConfigurer(meta.value()), a);
                });
    }

    private Configurer getConfigurer(Class<? extends Configurer> factoryClass) {
        return configurers.computeIfAbsent(factoryClass, c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException("Unable to instantiate configurer factory class " + factoryClass, e);
            }
        });
    }
}

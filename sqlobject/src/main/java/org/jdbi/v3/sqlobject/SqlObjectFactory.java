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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;

import static java.util.Collections.synchronizedMap;

/**
 * Creates implementations for SqlObject interfaces.
 */
public class SqlObjectFactory implements ExtensionFactory {
    private static final Object[] NO_ARGS = new Object[0];

    private final Map<Class<?>, Map<Method, Handler>> handlersCache = synchronizedMap(new WeakHashMap<>());
    private final Map<Class<? extends Configurer>, Configurer> configurers = synchronizedMap(new WeakHashMap<>());

    SqlObjectFactory() {}

    @Override
    public boolean accepts(Class<?> extensionType) {
        if (looksLikeSqlObject(extensionType)) {
            if (!extensionType.isInterface()) {
                throw new IllegalArgumentException("SQL Objects are only supported for interfaces.");
            }

            return true;
        }

        return false;
    }

    private boolean looksLikeSqlObject(Class<?> extensionType) {
        if (SqlObject.class.isAssignableFrom(extensionType)) {
            return true;
        }

        return Stream.of(extensionType.getMethods())
                .flatMap(m -> Stream.of(m.getAnnotations()))
                .anyMatch(a -> a.annotationType().isAnnotationPresent(SqlOperation.class));
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
        Map<Method, Handler> handlers = methodHandlersFor(
                extensionType,
                handle.getConfig(Handlers.class),
                handle.getConfig(HandlerDecorators.class));

        ConfigRegistry instanceConfig = handle.getConfig().createCopy();

        for (Class<?> iface : extensionType.getInterfaces()) {
            forEachConfigurer(iface, (configurer, annotation) ->
                configurer.configureForType(instanceConfig, annotation, extensionType));
        }
        forEachConfigurer(extensionType, (configurer, annotation) ->
                configurer.configureForType(instanceConfig, annotation, extensionType));

        InvocationHandler invocationHandler = createInvocationHandler(extensionType, instanceConfig, handlers, handle);
        return extensionType.cast(
                Proxy.newProxyInstance(
                        extensionType.getClassLoader(),
                        new Class[] {extensionType},
                        invocationHandler));
    }

    private Map<Method, Handler> methodHandlersFor(Class<?> sqlObjectType, Handlers registry, HandlerDecorators decorators) {
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
            } catch (IllegalStateException expected) {
                // optional implementation
            }

            for (Method method : sqlObjectType.getMethods()) {
                if (Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                handlers.computeIfAbsent(method, m -> buildMethodHandler(sqlObjectType, m, registry, decorators));
            }

            handlers.keySet().stream()
                .filter(m -> !m.isSynthetic())
                .collect(Collectors.groupingBy(m -> Arrays.asList(m.getName(), Arrays.asList(m.getParameterTypes()))))
                .values()
                .stream()
                .filter(l -> l.size() > 1)
                .findAny()
                .ifPresent(ms -> {
                    throw new UnableToCreateSqlObjectException(sqlObjectType + " has ambiguous methods " + ms + ", please resolve with an explicit override");
                });

            return handlers;
        });
    }

    private Handler buildMethodHandler(Class<?> sqlObjectType, Method method, Handlers handlers, HandlerDecorators decorators) {
        Handler handler = handlers.findFor(sqlObjectType, method)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "Method %s.%s must be default or be annotated with a SQL method annotation.",
                        sqlObjectType.getSimpleName(),
                        method.getName())));

        return decorators.applyDecorators(handler, sqlObjectType, method);
    }

    private static Map<Method, Handler> handlerEntry(Handler handler, Class<?> klass, String methodName, Class<?>... parameterTypes) {
        return Collections.singletonMap(Handlers.methodLookup(klass, methodName, parameterTypes), handler);
    }

    private InvocationHandler createInvocationHandler(Class<?> sqlObjectType,
                                                      ConfigRegistry instanceConfig,
                                                      Map<Method, Handler> handlers,
                                                      HandleSupplier handle) {
        Map<Method, ConfigRegistry> methodConfigs = new ConcurrentHashMap<>();

        Function<Method, ConfigRegistry> createConfigForMethod = method -> {
            ConfigRegistry config = instanceConfig.createCopy();
            forEachConfigurer(method, (configurer, annotation) -> configurer.configureForMethod(config, annotation, sqlObjectType, method));
            return config;
        };

        return (proxy, method, args) -> {
            ConfigRegistry methodConfig = methodConfigs.computeIfAbsent(method, createConfigForMethod).createCopy();
            Handler handler = handlers.get(method);

            return handle.invokeInContext(
                new ExtensionMethod(sqlObjectType, method),
                methodConfig,
                () -> handler.invoke(proxy, args == null ? NO_ARGS : args, handle)
            );
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

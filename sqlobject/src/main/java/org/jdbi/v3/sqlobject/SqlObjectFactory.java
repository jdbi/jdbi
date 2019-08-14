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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.JdbiCache;
import org.jdbi.v3.core.config.JdbiCaches;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData.InContextInvoker;

/**
 * Creates implementations for SqlObject interfaces.
 */
public class SqlObjectFactory implements ExtensionFactory {
    private final JdbiCache<Class<?>, SqlObjectInitData> sqlObjectCache =
            JdbiCaches.declare(SqlObjectFactory::initDataFor);

    SqlObjectFactory() {}

    @Override
    public boolean accepts(Class<?> extensionType) {
        if (looksLikeSqlObject(extensionType)) {
            if (extensionType.getAnnotation(GenerateSqlObject.class) != null) {
                return true;
            }

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
        ConfigRegistry instanceConfig = handle.getConfig().createCopy();

        SqlObjectInitData data = sqlObjectCache.get(extensionType, handle.getConfig());
        data.instanceConfigurer.apply(instanceConfig);

        if (data.concrete) {
            try {
                SqlObjectInitData.INIT_DATA.set(data);
                return extensionType.cast(
                    Class.forName(extensionType.getPackage().getName() + "." + extensionType.getSimpleName() + "Impl")
                        .getConstructor(HandleSupplier.class, ConfigRegistry.class)
                        .newInstance(handle, instanceConfig));
            } catch (ReflectiveOperationException | ExceptionInInitializerError e) {
                throw new UnableToCreateSqlObjectException(e);
            } finally {
                SqlObjectInitData.INIT_DATA.set(null);
            }
        }

        Map<Method, Supplier<InContextInvoker>> handlers = new HashMap<>();
        final Object proxy = Proxy.newProxyInstance(
                extensionType.getClassLoader(),
                new Class[] {extensionType},
                (p, m, a) -> handlers.get(m).get().invoke(a));

        data.methodHandlers.forEach((m, h) ->
                handlers.put(m, data.lazyInvoker(proxy, m, handle, instanceConfig)));
        return extensionType.cast(proxy);
    }

    private static Map<Method, Handler> buildMethodHandlers(
            Class<?> sqlObjectType,
            Handlers registry,
            HandlerDecorators decorators) {
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

        final Set<Method> methods = new LinkedHashSet<>();
        methods.addAll(Arrays.asList(sqlObjectType.getMethods()));
        methods.addAll(Arrays.asList(sqlObjectType.getDeclaredMethods()));

        final Set<Method> seen = handlers.keySet().stream()
                .collect(Collectors.toCollection(HashSet::new));
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) || !seen.add(method)) {
                continue;
            }
            handlers.put(method, decorators.applyDecorators(
                        registry.findFor(sqlObjectType, method)
                            .orElseGet(() -> {
                                Supplier<IllegalStateException> x = () -> new IllegalStateException(String.format(
                                        "Method %s.%s must be default or be annotated with a SQL method annotation.",
                                        sqlObjectType.getSimpleName(),
                                        method.getName()));
                                if (!SqlObjectInitData.isConcrete(sqlObjectType) && !method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                                    throw x.get();
                                }
                                return (t, a, h) -> {
                                    throw x.get();
                                };
                            }),
                        sqlObjectType,
                        method));
        }

        methods.stream()
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
    }

    private static Map<Method, Handler> handlerEntry(Handler handler, Class<?> klass, String methodName, Class<?>... parameterTypes) {
        return Collections.singletonMap(Handlers.methodLookup(klass, methodName, parameterTypes), handler);
    }

    private static UnaryOperator<ConfigRegistry> buildConfigurers(Stream<AnnotatedElement> elements, ConfigurerMethod consumer) {
        List<Consumer<ConfigRegistry>> myConfigurers = elements
                .flatMap(ae -> Arrays.stream(ae.getAnnotations()))
                .filter(a -> a.annotationType().isAnnotationPresent(ConfiguringAnnotation.class))
                .map(a -> {
                    ConfiguringAnnotation meta = a.annotationType()
                            .getAnnotation(ConfiguringAnnotation.class);

                    Configurer configurer = getConfigurer(meta.value());
                    return (Consumer<ConfigRegistry>) config -> consumer.configure(configurer, config, a);
                })
                .collect(Collectors.toList());
        return config -> {
            myConfigurers.forEach(configurer -> configurer.accept(config));
            return config;
        };
    }

    private static Configurer getConfigurer(Class<? extends Configurer> factoryClass) {
        try {
            return factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate configurer factory class " + factoryClass, e);
        }
    }

    static SqlObjectInitData initDataFor(ConfigRegistry handlersConfig, Class<?> sqlObjectType) {
        Map<Method, Handler> methodHandlers = buildMethodHandlers(
                sqlObjectType,
                handlersConfig.get(Handlers.class),
                handlersConfig.get(HandlerDecorators.class));

        UnaryOperator<ConfigRegistry> instanceConfigurer = buildConfigurers(
                Stream.concat(
                    Arrays.stream(sqlObjectType.getInterfaces()),
                    Stream.of(sqlObjectType)),
                (configurer, config, annotation) ->
                    configurer.configureForType(config, annotation, sqlObjectType));

        Map<Method, UnaryOperator<ConfigRegistry>> methodConfigurers =
            methodHandlers.keySet().stream().collect(
                Collectors.toMap(Function.identity(),
                method -> buildConfigurers(
                    Stream.of(method),
                    (configurer, config, annotation) ->
                        configurer.configureForMethod(config, annotation, sqlObjectType, method))));

        return new SqlObjectInitData(
                sqlObjectType,
                instanceConfigurer,
                methodConfigurers,
                methodHandlers);
    }

    interface ConfigurerMethod {
        void configure(Configurer configurer, ConfigRegistry config, Annotation annotation);
    }
}

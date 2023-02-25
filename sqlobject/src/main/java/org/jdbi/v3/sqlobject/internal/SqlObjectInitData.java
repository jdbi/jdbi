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
package org.jdbi.v3.sqlobject.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.internal.ConfigCache;
import org.jdbi.v3.core.config.internal.ConfigCaches;
import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.MemoizingSupplier;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.sqlobject.GenerateSqlObject;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.HandlerDecorators;
import org.jdbi.v3.sqlobject.Handlers;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;
import org.jdbi.v3.sqlobject.config.Configurer;
import org.jdbi.v3.sqlobject.config.ConfiguringAnnotation;

import static java.lang.String.format;

import static org.jdbi.v3.sqlobject.Handler.EQUALS_HANDLER;
import static org.jdbi.v3.sqlobject.Handler.GET_HANDLE_HANDLER;
import static org.jdbi.v3.sqlobject.Handler.HASHCODE_HANDLER;
import static org.jdbi.v3.sqlobject.Handler.NULL_HANDLER;
import static org.jdbi.v3.sqlobject.Handler.WITH_HANDLE_HANDLER;

public final class SqlObjectInitData {

    // This should be a field in InContextInvoker but static fields are not allowed in inner classes in Java 8
    private static final Object[] NO_ARGS = new Object[0];

    private static final ConfigCache<Class<?>, SqlObjectInitData> INIT_DATA_CACHE =
            ConfigCaches.declare(SqlObjectInitData::initDataFor);
    private final Class<?> extensionType;
    private final UnaryOperator<ConfigRegistry> instanceConfigurer;
    private final Map<Method, UnaryOperator<ConfigRegistry>> methodConfigurers;
    private final Map<Method, Handler> methodHandlers;

    private SqlObjectInitData(
            Class<?> extensionType,
            UnaryOperator<ConfigRegistry> instanceConfigurer,
            Map<Method, UnaryOperator<ConfigRegistry>> methodConfigurers,
            Map<Method, Handler> methodHandlers) {
        this.extensionType = extensionType;
        this.instanceConfigurer = instanceConfigurer;
        this.methodConfigurers = methodConfigurers;
        this.methodHandlers = methodHandlers;
    }

    public static boolean isConcrete(Class<?> extensionTypeClass) {
        return extensionTypeClass.getAnnotation(GenerateSqlObject.class) != null;
    }

    public static SqlObjectInitData lookup(Class<?> key, ConfigRegistry config) {
        return INIT_DATA_CACHE.get(key, config);
    }

    public Class<?> extensionType() {
        return extensionType;
    }

    public ConfigRegistry configureInstance(ConfigRegistry config) {
        return instanceConfigurer.apply(config);
    }

    public void forEachMethodHandler(BiConsumer<Method, Handler> action) {
        methodHandlers.forEach(action);
    }

    public InContextInvoker getInvoker(Object target, Method method, HandleSupplier handleSupplier, ConfigRegistry instanceConfig) {
        return new InContextInvoker(target, method, handleSupplier, instanceConfig);
    }

    private static SqlObjectInitData initDataFor(ConfigRegistry handlersConfig, Class<?> sqlObjectType) {
        Map<Method, Handler> methodHandlers = buildMethodHandlers(handlersConfig, sqlObjectType);

        // process all annotations on the type.
        final ConfigurerMethod forType = (configurer, config, annotation) -> configurer.configureForType(config, annotation, sqlObjectType);

        // build a configurer for the type and all supertypes. This processes all annotations on classes and interfaces
        UnaryOperator<ConfigRegistry> instanceConfigurer = buildConfigurers(
                Stream.concat(JdbiClassUtils.superTypes(sqlObjectType), Stream.of(sqlObjectType)), forType);

        // A map for all methods that contains the annotations for each method on the type
        Map<Method, UnaryOperator<ConfigRegistry>> methodConfigurers = methodHandlers.keySet().stream().collect(Collectors.toMap(
                        Function.identity(),
                        method -> {
                            // process all annotations on a single method.
                            final ConfigurerMethod forMethod = (configurer, config, annotation) ->
                                    configurer.configureForMethod(config, annotation, sqlObjectType, method);
                            // build a configurer that processes all annotations on the method itself.
                            return buildConfigurers(Stream.of(method), forMethod);
                        }));

        return new SqlObjectInitData(
                sqlObjectType,
                instanceConfigurer,
                methodConfigurers,
                methodHandlers);
    }

    private static Map<Method, Handler> buildMethodHandlers(
            ConfigRegistry config,
            Class<?> sqlObjectType) {

        final Handlers handlers = config.get(Handlers.class);
        final HandlerDecorators handlerDecorators = config.get(HandlerDecorators.class);
        final Map<Method, Handler> handlerMap = new HashMap<>();

        // tostring can not be constant, different for every object
        Handler toStringHandler = (target, args, handleSupplier) ->
                "Jdbi sqlobject proxy for " + sqlObjectType.getName() + "@" + Integer.toHexString(target.hashCode());

        addMethodHandler(handlerMap, toStringHandler, Object.class, "toString");
        addMethodHandler(handlerMap, EQUALS_HANDLER, Object.class, "equals", Object.class);
        addMethodHandler(handlerMap, HASHCODE_HANDLER, Object.class, "hashCode");

        // SQL Object methods. Add those unconditionally, this ensures that any generator class
        // that implements SqlObject when the interface does not will find methods to execute.
        // for any proxy interface that does not extend SqlObject, these methods are unreachable and
        // do no harm.
        addMethodHandler(handlerMap, GET_HANDLE_HANDLER, SqlObject.class, "getHandle");
        addMethodHandler(handlerMap, WITH_HANDLE_HANDLER, SqlObject.class, "withHandle", HandleCallback.class);

        addMethodHandler(handlerMap, NULL_HANDLER, sqlObjectType, "finalize");

        final Set<Method> methods = new LinkedHashSet<>();
        methods.addAll(Arrays.asList(sqlObjectType.getMethods()));
        methods.addAll(Arrays.asList(sqlObjectType.getDeclaredMethods()));

        final Set<Method> seen = new HashSet<>(handlerMap.keySet());
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers()) || !seen.add(method)) {
                continue;
            }
            handlerMap.put(method, handlerDecorators.applyDecorators(
                        handlers.findFor(sqlObjectType, method)
                            .orElseGet(() -> {
                                Supplier<IllegalStateException> x = () -> new IllegalStateException(format(
                                        "Method %s.%s must have an implementation or be annotated with a SQL method annotation.",
                                        method.getDeclaringClass().getSimpleName(),
                                        method.getName()));
                                if (!SqlObjectInitData.isConcrete(sqlObjectType) && !method.isSynthetic() && !Modifier.isPrivate(method.getModifiers())) {
                                    throw x.get();
                                }
                                return (target, args, handleSupplier) -> {
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

        return handlerMap;
    }

    private static void addMethodHandler(Map<Method, Handler> handlerMap, Handler handler, Class<?> klass, String methodName, Class<?>... parameterTypes) {
        JdbiClassUtils.safeMethodLookup(klass, methodName, parameterTypes)
                .ifPresent(method -> handlerMap.put(method, handler));
    }

    private static UnaryOperator<ConfigRegistry> buildConfigurers(Stream<AnnotatedElement> elements, ConfigurerMethod consumer) {
        List<Consumer<ConfigRegistry>> myConfigurers = elements
                .flatMap(ae -> Arrays.stream(ae.getAnnotations()))
                .filter(a -> a.annotationType().isAnnotationPresent(ConfiguringAnnotation.class))
                .map(a -> {
                    ConfiguringAnnotation meta = a.annotationType().getAnnotation(ConfiguringAnnotation.class);
                    Class<? extends Configurer> klass = meta.value();

                    try {
                        Configurer configurer = klass.getConstructor().newInstance();
                        return (Consumer<ConfigRegistry>) config -> consumer.configure(configurer, config, a);
                    } catch (ReflectiveOperationException | SecurityException e) {
                        throw new IllegalStateException(format("Unable to instantiate class %s", klass), e);
                    }
                })
                .collect(Collectors.toList());
        return config -> {
            myConfigurers.forEach(configurer -> configurer.accept(config));
            return config;
        };
    }

    private interface ConfigurerMethod {
        void configure(Configurer configurer, ConfigRegistry config, Annotation annotation);
    }

    public final class InContextInvoker {
        private final Object target;
        private final HandleSupplier handleSupplier;
        private final ExtensionContext extensionContext;
        private final Supplier<Handler> supplier;

        InContextInvoker(Object target, Method method, HandleSupplier handleSupplier, ConfigRegistry config) {
            this.target = target;
            this.handleSupplier = handleSupplier;
            ConfigRegistry methodConfig = methodConfigurers.get(method).apply(config.createCopy());
            this.extensionContext = ExtensionContext.forExtensionMethod(methodConfig, extensionType, method);
            this.supplier = MemoizingSupplier.of(() -> {
                Handler methodHandler = methodHandlers.get(method);
                methodHandler.warm(methodConfig);
                return methodHandler;
            });
        }

        public Object invoke(Object... args) {
            Handler handler = supplier.get();
            final Callable<Object> callable = () -> handler.invoke(target, args == null ? NO_ARGS : args, handleSupplier);
            return call(callable);
        }

        public Object call(Callable<?> callable) {
            try {
                return handleSupplier.invokeInContext(extensionContext, callable);
            } catch (Exception x) {
                throw Sneaky.throwAnyway(x);
            }
        }

        public Object call(Runnable runnable) {
            return call(() -> {
                runnable.run();
                return null;
            });
        }
    }
}

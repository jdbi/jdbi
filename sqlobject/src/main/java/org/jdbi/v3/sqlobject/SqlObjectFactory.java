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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jdbi.v3.Handle;
import org.jdbi.v3.extension.ExtensionFactory;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.mixins.Transactional;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.MethodInterceptor;

public enum SqlObjectFactory implements ExtensionFactory<SqlObject> {
    INSTANCE;

    private static final MethodInterceptor NO_OP = (proxy, method, args, methodProxy) -> null;

    private final Map<Method, Handler> mixinHandlers = new HashMap<>();
    private final ConcurrentMap<Class<?>, Map<Method, Handler>> handlersCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Factory> factories = new ConcurrentHashMap<>();

    SqlObjectFactory() {
        mixinHandlers.putAll(TransactionalHelper.handlers());
        mixinHandlers.putAll(GetHandleHelper.handlers());
    }

    @Override
    public SqlObject createConfig() {
        return new SqlObject();
    }

    @Override
    public boolean accepts(Class<?> extensionType) {
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
    public <E> E attach(Class<E> extensionType, SqlObject config, Supplier<Handle> handle) {
        Factory f = factories.computeIfAbsent(extensionType, type -> {
            Enhancer e = new Enhancer();
            e.setClassLoader(extensionType.getClassLoader());

            List<Class<?>> interfaces = new ArrayList<>();
            if (extensionType.isInterface()) {
                interfaces.add(extensionType);
            }
            else {
                e.setSuperclass(extensionType);
            }
            e.setInterfaces(interfaces.toArray(new Class[interfaces.size()]));
            e.setCallback(NO_OP);

            return (Factory) e.create();
        });

        Map<Method, Handler> handlers = buildHandlersFor(extensionType, config);
        MethodInterceptor interceptor = createMethodInterceptor(handlers, handle);
        return extensionType.cast(f.newInstance(interceptor));
    }

    private Map<Method, Handler> buildHandlersFor(Class<?> sqlObjectType, SqlObject config) {
        return handlersCache.computeIfAbsent(sqlObjectType, type -> {

            final Map<Method, Handler> handlers = new HashMap<>();
            for (Method method : sqlObjectType.getMethods()) {
                Optional<? extends Class<? extends HandlerFactory>> factoryClass = Stream.of(method.getAnnotations())
                        .map(a -> a.annotationType().getAnnotation(SqlMethodAnnotation.class))
                        .filter(Objects::nonNull)
                        .map(a -> a.value())
                        .findFirst();

                if (factoryClass.isPresent()) {
                    HandlerFactory factory = buildFactory(factoryClass.get());
                    Handler handler = factory.buildHandler(sqlObjectType, method, config);
                    handlers.put(method, handler);
                }
                else if (mixinHandlers.containsKey(method)) {
                    handlers.put(method, mixinHandlers.get(method));
                }
                else {
                    handlers.put(method, new PassThroughHandler());
                }
            }

            handlers.putAll(EqualsHandler.handler());
            handlers.putAll(ToStringHandler.handler(sqlObjectType.getName()));
            handlers.putAll(HashCodeHandler.handler());

            return handlers;
        });
    }

    private HandlerFactory buildFactory(Class<? extends HandlerFactory> factoryClazz) {
        HandlerFactory factory;
        try {
            factory = factoryClazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Factory class " + factoryClazz + "cannot be instantiated", e);
        }
        return factory;
    }

    private MethodInterceptor createMethodInterceptor(Map<Method, Handler> handlers, Supplier<Handle> handle) {
        return (proxy, method, args, methodProxy) -> {
            Handler handler = handlers.get(method);

            // If there is no handler, pretend we are just an Object and don't open a connection (Issue #82)
            if (handler == null) {
                return methodProxy.invokeSuper(proxy, args);
            }

            return handler.invoke(handle, proxy, args, method);
        };
    }
}

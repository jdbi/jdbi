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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionContext;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.MemoizingSupplier;
import org.jdbi.v3.core.internal.exceptions.Sneaky;
import org.jdbi.v3.sqlobject.GenerateSqlObject;
import org.jdbi.v3.sqlobject.Handler;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.UnableToCreateSqlObjectException;

public final class SqlObjectInitData {
    private static final Object[] NO_ARGS = new Object[0];
    public static final ThreadLocal<SqlObjectInitData> INIT_DATA = new ThreadLocal<>();

    private final boolean concrete;
    private final Class<?> extensionType;
    private final UnaryOperator<ConfigRegistry> instanceConfigurer;
    private final Map<Method, UnaryOperator<ConfigRegistry>> methodConfigurers;
    private final Map<Method, Handler> methodHandlers;

    public SqlObjectInitData(
            Class<?> extensionType,
            UnaryOperator<ConfigRegistry> instanceConfigurer,
            Map<Method, UnaryOperator<ConfigRegistry>> methodConfigurers,
            Map<Method, Handler> methodHandlers) {
        concrete = isConcrete(extensionType);
        this.extensionType = extensionType;
        this.instanceConfigurer = instanceConfigurer;
        this.methodConfigurers = methodConfigurers;
        this.methodHandlers = methodHandlers;
    }

    public static boolean isConcrete(Class<?> extensionType) {
        return extensionType.getAnnotation(GenerateSqlObject.class) != null;
    }

    public static SqlObjectInitData initData() {
        final SqlObjectInitData result = INIT_DATA.get();
        if (result == null) {
            throw new IllegalStateException("Implemented SqlObject types must be initialized by SqlObjectFactory");
        }
        return result;
    }

    public static Method lookupMethod(String methodName, Class<?>... parameterTypes) {
        return lookupMethod(initData().extensionType, methodName, parameterTypes);
    }

    private static Method lookupMethod(Class<?> klass, String methodName, Class<?>... parameterTypes) {
        try {
            return klass.getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            try {
                return klass.getDeclaredMethod(methodName, parameterTypes);
            } catch (Exception x) {
                e.addSuppressed(x);
                try {
                    return SqlObject.class.getMethod(methodName, parameterTypes);
                } catch (Exception x2) {
                    e.addSuppressed(x2);
                }
            }
            throw new IllegalStateException(
                    String.format("can't find %s#%s%s", klass.getName(), methodName, Arrays.asList(parameterTypes)), e);
        }
    }

    public boolean isConcrete() {
        return concrete;
    }

    public Class<?> extensionType() {
        return extensionType;
    }

    public <E> E instantiate(Class<E> passExtensionType, HandleSupplier handleSupplier, ConfigRegistry instanceConfig) {
        if (!extensionType.equals(passExtensionType)) {
            throw new IllegalArgumentException("mismatch extension type");
        }

        try {
            SqlObjectInitData.INIT_DATA.set(this);
            return passExtensionType.cast(
                Class.forName(extensionType.getPackage().getName() + "." + extensionType.getSimpleName() + "Impl")
                    .getConstructor(HandleSupplier.class, ConfigRegistry.class)
                    .newInstance(handleSupplier, instanceConfig));
        } catch (Exception | ExceptionInInitializerError e) {
            throw new UnableToCreateSqlObjectException(e);
        } finally {
            SqlObjectInitData.INIT_DATA.remove();
        }
    }

    public ConfigRegistry configureInstance(ConfigRegistry config) {
        return instanceConfigurer.apply(config);
    }

    public void forEachMethodHandler(BiConsumer<Method, Handler> action) {
        methodHandlers.forEach(action);
    }

    public Supplier<InContextInvoker> lazyInvoker(Object target, Method method, HandleSupplier handleSupplier, ConfigRegistry instanceConfig) {
        return MemoizingSupplier.of(() -> new InContextInvoker(target, method, handleSupplier, instanceConfig));
    }

    public final class InContextInvoker {
        private final Object target;
        private final HandleSupplier handleSupplier;
        private final ExtensionContext extensionContext;
        private final Handler methodHandler;

        InContextInvoker(Object target, Method method, HandleSupplier handleSupplier, ConfigRegistry config) {
            this.target = target;
            this.handleSupplier = handleSupplier;
            ConfigRegistry methodConfig = methodConfigurers.get(method).apply(config.createCopy());
            this.extensionContext = ExtensionContext.forExtensionMethod(methodConfig, extensionType, method);
            this.methodHandler = methodHandlers.get(method);

            methodHandler.warm(methodConfig);
        }

        public Object invoke(Object[] args) {
            final Callable<Object> callable = () -> methodHandler.invoke(target, args == null ? NO_ARGS : args, handleSupplier);
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

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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.internal.exceptions.Sneaky;
import org.jdbi.meta.Alpha;

import static java.lang.String.format;

/**
 * Provides functionality for a single method on an extension object. Each extension handler can either
 * call another piece of code to return the result (e.g. the method on the underlying object) or return the
 * result itself.
 *
 * @since 3.38.0
 */
@FunctionalInterface
@Alpha
public interface ExtensionHandler {

    /**
     * Attach this extension handler to a target instance.
     * @param config the configuration at time of attach
     * @param target the target object on which the handler should operate
     */
    AttachedExtensionHandler attachTo(ConfigRegistry config, Object target);

    /**
     * Returns a default handler for missing functionality. The handler will throw an exception when invoked.
     * @param method The method to which this specific handler instance is bound
     * @return An {@link ExtensionHandler} instance
     */
    static ExtensionHandler missingExtensionHandler(Method method) {
        return (ExtensionHandler.Simple) (handleSupplier, args) -> {
            throw new IllegalStateException(format(
                    "Method %s.%s has no registered extension handler!",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName()));
        };
    }

    /**
     * Create an extension handler and bind it to a method that will be called on the
     * target object when invoked.
     * @param method The {@link Method} to bind to
     * @return An {@link ExtensionHandler}
     * @throws IllegalAccessException If the method could not be unreflected
     */
    static ExtensionHandler createForMethod(Method method) throws IllegalAccessException {
        final Class<?> declaringClass = method.getDeclaringClass();
        final MethodHandle methodHandle = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup()).unreflect(method);
        return createForMethodHandle(methodHandle);
    }

    /**
     * Create an extension handler and bind it to a special method that will be called on the
     * target object when invoked. This is needed e.g. for interface default methods.
     * @param method The {@link Method} to bind to
     * @return An {@link ExtensionHandler}
     * @throws IllegalAccessException If the method could not be unreflected
     */
    static ExtensionHandler createForSpecialMethod(Method method) throws IllegalAccessException {
        final Class<?> declaringClass = method.getDeclaringClass();
        final MethodHandle methodHandle = MethodHandles.privateLookupIn(declaringClass, MethodHandles.lookup()).unreflectSpecial(method, declaringClass);
        return createForMethodHandle(methodHandle);
    }

    /**
     * Create an extension handler and bind it to a {@link MethodHandle} instance.
     * @param methodHandle The {@link MethodHandle} to bind to
     * @return An {@link ExtensionHandler}
     */
    static ExtensionHandler createForMethodHandle(MethodHandle methodHandle) {
        return new ExtensionHandler() {
            @Override
            public AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
                if (target == null) {
                    throw new IllegalStateException("no target object present, called from a proxy factory?");
                }
                return new AttachedExtensionHandler() {
                    final MethodHandle boundMh = methodHandle.bindTo(target);
                    @Override
                    public Object invoke(HandleSupplier handleSupplier, Object... args) throws Exception {
                        try {
                            return boundMh.invokeWithArguments(args);
                        } catch (Throwable e) {
                            throw Sneaky.throwAnyway(e);
                        }
                    }
                };
            }
        };
    }

    @FunctionalInterface
    interface Simple extends ExtensionHandler, AttachedExtensionHandler {
        @Override
        default AttachedExtensionHandler attachTo(ConfigRegistry config, Object target) {
            return this;
        }
    }
}

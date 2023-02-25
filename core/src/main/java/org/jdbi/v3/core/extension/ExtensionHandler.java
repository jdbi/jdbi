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
package org.jdbi.v3.core.extension;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Optional;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.meta.Alpha;
import org.jdbi.v3.meta.Beta;

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

    /** Implementation for the {@link Object#equals(Object)} method. Each object using this handler is only equal to ifself. */
    ExtensionHandler EQUALS_HANDLER = (handleSupplier, target, args) -> target == args[0];

    /** Implementation for the {@link Object#hashCode()} method. */
    ExtensionHandler HASHCODE_HANDLER = (handleSupplier, target, args) -> System.identityHashCode(target);

    /** Handler that only returns null independent of any input parameters. */
    ExtensionHandler NULL_HANDLER = (handleSupplier, target, args) -> null;

    /**
     * Gets invoked to return a value for the method that this handler was bound to.
     * @param handleSupplier A {@link HandleSupplier} instance for accessing the handle and its related objects.
     * @param target The target object on which the handler should operate.
     * @param args Optional arguments for the handler.
     * @return The return value for the method that was bound to the extension handler. Can be null.
     * @throws Exception Any exception from the underlying code.
     */
    Object invoke(HandleSupplier handleSupplier, Object target, Object... args) throws Exception;

    /**
     * Called after the method handler is constructed to pre-initialize any important
     * configuration data structures.
     *
     * @param config the method configuration to use for warming up.
     */
    @Beta
    default void warm(ConfigRegistry config) {}

    /**
     * Returns a default handler for missing functionality. The handler will throw an exception when invoked.
     * @param method The method to which this specific handler instance is bound.
     * @return An {@link ExtensionHandler} instance.
     */
    static ExtensionHandler missingExtensionHandler(Method method) {
        return (target, args, handleSupplier) -> {
            throw new IllegalStateException(format(
                    "Method %s.%s has no registered extension handler!",
                    method.getDeclaringClass().getSimpleName(),
                    method.getName()));
        };
    }

    /**
     * Create an extension handler and bind it to a method that will be called on the
     * target object when invoked.
     * @param method The {@link Method} to bind to.
     * @return An {@link ExtensionHandler}.
     * @throws IllegalAccessException If the method could not be unreflected.
     */
    static ExtensionHandler createForMethod(Method method) throws IllegalAccessException {
        Class<?> declaringClass = method.getDeclaringClass();
        final MethodHandle methodHandle = PrivateLookupInKludge.lookupFor(declaringClass).unreflect(method);
        return createForMethodHandle(methodHandle);
    }

    /**
     * Create an extension handler and bind it to a special method that will be called on the
     * target object when invoked. This is needed e.g. for interface default methods.
     * @param method The {@link Method} to bind to.
     * @return An {@link ExtensionHandler}.
     * @throws IllegalAccessException If the method could not be unreflected.
     */
    static ExtensionHandler createForSpecialMethod(Method method) throws IllegalAccessException {
        Class<?> declaringClass = method.getDeclaringClass();
        final MethodHandle methodHandle = PrivateLookupInKludge.lookupFor(declaringClass).unreflectSpecial(method, declaringClass);
        return createForMethodHandle(methodHandle);
    }

    /**
     * Create an extension handler and bind it to a {@link MethodHandle} instance.
     * @param methodHandle The {@link MethodHandle} to bind to.
     * @return An {@link ExtensionHandler}.
     * @throws IllegalAccessException If the method could not be unreflected.
     */
    static ExtensionHandler createForMethodHandle(MethodHandle methodHandle) {
        return (handleSupplier, target, args) -> {
            if (target == null) {
                throw new IllegalStateException("no target object present, called from a proxy factory?");
            }
            return Unchecked.<Object[], Object>function(methodHandle.bindTo(target)::invokeWithArguments).apply(args);
        };
    }

    /**
     * A factory to create {@link ExtensionHandler} instances.
     */
    interface ExtensionHandlerFactory {

        /**
         * Determines whether the factory can create an {@link ExtensionHandler} for combination of extension type and method.
         *
         * @param extensionType The extension type class.
         * @param method A method.
         * @return True if the factory can create an extension handler for extension type and method, false otherwise.
         */
        boolean accepts(Class<?> extensionType, Method method);

        /**
         * Returns an {@link ExtensionHandler} for a extension type and method combination.
         * @param extensionType The extension type class.
         * @param method A method.
         * @return An {@link ExtensionHandler} instance wrapped into an {@link Optional}. The optional can be empty. This is necessary to retrofit old code
         * that does not have an accept/build code pair but unconditionally tries to build a handler and returns empty if it can not. New code should always
         * return <code>Optional.of(extensionHandler}</code> and never return <code>Optional.empty()</code>.
         */
        Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method);
    }
}

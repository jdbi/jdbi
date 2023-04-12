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

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * A factory to create {@link ExtensionHandler} instances.
 */
public interface ExtensionHandlerFactory {

    /**
     * Determines whether the factory can create an {@link ExtensionHandler} for combination of extension type and method.
     *
     * @param extensionType The extension type class
     * @param method        A method
     * @return True if the factory can create an extension handler for extension type and method, false otherwise
     */
    boolean accepts(Class<?> extensionType, Method method);

    /**
     * Returns an {@link ExtensionHandler} instance for a extension type and method combination.
     *
     * @param extensionType The extension type class
     * @param method        A method
     * @return An {@link ExtensionHandler} instance wrapped into an {@link Optional}. The optional can be empty. This is necessary to retrofit old code
     * that does not have an accept/build code pair but unconditionally tries to build a handler and returns empty if it can not. New code should always
     * return <code>Optional.of(extensionHandler}</code> and never return <code>Optional.empty()</code>
     */
    Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method);
}

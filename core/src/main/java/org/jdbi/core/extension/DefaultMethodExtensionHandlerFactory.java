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

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Provides {@link ExtensionHandler} instances for interface default methods.
 */
final class DefaultMethodExtensionHandlerFactory implements ExtensionHandlerFactory {

    static final ExtensionHandlerFactory INSTANCE = new DefaultMethodExtensionHandlerFactory();

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {
        return extensionType.isInterface() && method.isDefault();  // interface default method
    }

    @Override
    public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {
        try {
            return Optional.of(ExtensionHandler.createForSpecialMethod(method));
        } catch (IllegalAccessException e) {
            throw new UnableToCreateExtensionException(e, "Default method handler for %s couldn't unreflect %s", extensionType, method);
        }
    }
}

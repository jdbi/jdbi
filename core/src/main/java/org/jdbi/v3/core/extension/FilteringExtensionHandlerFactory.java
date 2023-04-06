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

final class FilteringExtensionHandlerFactory implements ExtensionHandlerFactory {

    private final ExtensionHandlerFactory delegate;

    static ExtensionHandlerFactory forDelegate(ExtensionHandlerFactory delegate) {
        return new FilteringExtensionHandlerFactory(delegate);
    }

    private FilteringExtensionHandlerFactory(ExtensionHandlerFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {
        if (method == null || method.isSynthetic() || method.isBridge()) {
            return false;
        }

        return delegate.accepts(extensionType, method);
    }

    @Override
    public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {
        return delegate.createExtensionHandler(extensionType, method);
    }
}

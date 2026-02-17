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

import org.jdbi.meta.Alpha;

/**
 * Supports customization of an extension handler. Common use cases are decorators through annotations on the method
 * itself. The SqlObject extension implements its annotations through {@link ExtensionHandlerCustomizer} instances.
 *
 * @since 3.38.0
 */
@Alpha
@FunctionalInterface
public interface ExtensionHandlerCustomizer {

    /**
     * Customize an extension handler.
     *
     * @param handler       The {@link ExtensionHandler} to customize
     * @param extensionType The extension type class
     * @param method        A method
     * @return An {@link ExtensionHandler} object. This can be the same as the <code>handler</code> parameter
     * or another instance that delegates to the original handler
     */
    ExtensionHandler customize(ExtensionHandler handler, Class<?> extensionType, Method method);
}

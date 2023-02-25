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
import java.lang.reflect.Modifier;
import java.util.Optional;

import org.jdbi.v3.core.extension.ExtensionHandler.ExtensionHandlerFactory;

/**
 * Provides {@link ExtensionHandler} instances for all methods that have not been covered in
 * any other way. It forwards a call to the handler to a method invocation on the target
 * object. For any extension factory that simply provides an implementation of the extension
 * interface, this forwards the call to the method on the implementation. The extension framework
 * wraps these calls into invocations that manage the extension context for the handle correctly
 * so that logging will work for all extension.
 *
 * @since 3.38.0
 */
final class InstanceExtensionHandlerFactory implements ExtensionHandlerFactory {

    static final ExtensionHandlerFactory INSTANCE = new InstanceExtensionHandlerFactory();

    @Override
    public boolean accepts(Class<?> extensionType, Method method) {
        return Modifier.isAbstract(method.getModifiers())                                        // any abstract method
                || (!extensionType.isInterface() && method.getDeclaringClass() != Object.class); // any non-interface type if the method is not from Object
    }

    @Override
    public Optional<ExtensionHandler> buildExtensionHandler(Class<?> extensionType, Method method) {
        try {
            return Optional.of(ExtensionHandler.createForMethod(method));
        } catch (IllegalAccessException e) {
            throw new UnableToCreateExtensionException(e, "Instance handler for %s couldn't unreflect %s", extensionType, method);
        }
    }
}

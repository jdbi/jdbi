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
import java.util.Optional;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.extension.ExtensionMethod;

class WithHandleMethodHandlerFactory implements HandlerFactory {
    private static final Method WITH_HANDLE = Handlers.methodLookup(SqlObject.class, "withHandle", HandleCallback.class);

    @SuppressWarnings("unchecked")
    @Override
    public Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method) {
        if (!WITH_HANDLE.equals(method)) {
            return Optional.empty();
        }
        ExtensionMethod extensionMethod = new ExtensionMethod(sqlObjectType, method);
        return Optional.of((t, a, h) ->
            h.invokeInContext(
                    extensionMethod,
                    h.getConfig(),
                    () -> ((HandleCallback<?, RuntimeException>) a[0]).withHandle(h.getHandle())));
    }
}

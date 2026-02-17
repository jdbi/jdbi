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
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

/**
 * Holder for a {@link Class} and a {@link Method} that together
 * define which extension method was invoked.
 */
public final class ExtensionMethod {
    private final Class<?> type;
    private final Method method;

    /**
     * Creates a new extension method.
     * @param type the type the method was invoked on
     * @param method the method invoked
     */
    public ExtensionMethod(Class<?> type, Method method) {
        this.type = requireNonNull(type);
        this.method = requireNonNull(method);
    }

    /**
     * Returns the type the method was invoked on.
     *
     * @return the type the method was invoked on.
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * Returns the method invoked.
     *
     * @return the method invoked.
     */
    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExtensionMethod.class.getSimpleName() + "[", "]")
            .add("type=" + type)
            .add("method=" + method)
            .toString();
    }
}

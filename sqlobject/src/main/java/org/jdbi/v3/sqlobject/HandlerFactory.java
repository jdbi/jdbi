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

/**
 * Creates Handler objects for methods annotated with a specific SQL method annotation, which satisfy the contract of
 * that annotation.
 *
 * @see SqlOperation
 */
public interface HandlerFactory {
    /**
     * Returns a {@link Handler} instance for executing the given SQL Object method.
     *
     * @param sqlObjectType the SQL Object type
     * @param method        the method
     * @return a handler, if applicable
     */
    Optional<Handler> buildHandler(Class<?> sqlObjectType, Method method);
}

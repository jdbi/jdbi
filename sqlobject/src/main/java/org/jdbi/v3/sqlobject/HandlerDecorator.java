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

/**
 * Creates Handler decorator objects for methods annotated with a specific SQL method <em>decorating</em> annotation,
 * which satisfy the contract of that annotation.
 *
 * @see SqlMethodDecoratingAnnotation
 */
public interface HandlerDecorator {
    /**
     * Decorates a {@link Handler} to add or substitute behavior on the given SQL Object method.
     *
     * @param base          the base handler to decorate
     * @param sqlObjectType the SQL Object type
     * @param method        the decorated method
     * @return the decorated handler.
     */
    Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method);
}

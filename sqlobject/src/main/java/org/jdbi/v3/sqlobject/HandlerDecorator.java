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
 * Decorates Handler objects with additional behavior.
 *
 * @see SqlMethodDecoratingAnnotation
 * @see HandlerDecorators
 */
public interface HandlerDecorator {
    /**
     * Decorates the {@link Handler} to add or substitute behavior on the given SQL Object method. Implementations may
     * alternatively return the base handler, e.g. if the conditions for applying a particular decoration are not met.
     *
     * @param base          the base handler to decorate
     * @param sqlObjectType the SQL Object type
     * @param method        the method to be decorated
     * @return the base handle, or a decorated handler (depending on the decorator implementation).
     */
    Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method);
}

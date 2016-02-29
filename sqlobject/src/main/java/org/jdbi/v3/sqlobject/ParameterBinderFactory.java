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
 * Factory interface used to produce result set mappers.
 */
public interface ParameterBinderFactory
{
    /**
     * Can this factory provide a parameter binder for binding a method parameter to a sql bound parameter?
     * @param type the class containing the method
     * @param method the method that requires binders
     * @param param_idx the current parameter index that will be bound
     * @return true if it can, false if it cannot
     */
    boolean accepts(Class type, Method method, int param_idx);

    /**
     * Supplies a binder for the given parameter, or null to fallback to a default binder
     */
    Binder binderFor(Class type, Method method, int param_idx);
}

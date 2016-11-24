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

import org.jdbi.v3.core.HandleSupplier;

/**
 * Implements the contract of a SQL Object method.
 */
public interface Handler {
    /**
     * Executes a SQL Object method, and returns the result.
     *
     * @param target the SQL Object instance being invoked
     * @param method the method being invoked on the SQL Object.
     * @param args   the arguments that were passed to the method.
     * @param handle a (possibly lazy) Handle supplier.
     * @return the method return value, or null if the method has a void return type.
     * @throws Exception any exception thrown by the method.
     */
    Object invoke(Object target, Method method, Object[] args, HandleSupplier handle) throws Exception;
}

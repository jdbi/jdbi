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

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.meta.Beta;

/**
 * Implements the contract of a SQL Object method.
 */
@FunctionalInterface
public interface Handler {

    Handler EQUALS_HANDLER = (target, args, handleSupplier) -> target == args[0];
    Handler HASHCODE_HANDLER = (target, args, handleSupplier) -> System.identityHashCode(target);
    Handler GET_HANDLE_HANDLER = (target, args, handleSupplier) -> handleSupplier.getHandle();

    Handler WITH_HANDLE_HANDLER = (target, args, handleSupplier) -> ((HandleCallback<?, RuntimeException>) args[0]).withHandle(handleSupplier.getHandle());
    Handler NULL_HANDLER = (target, args, handleSupplier) -> null;

    /**
     * Executes a SQL Object method, and returns the result.
     *
     * @param target         the SQL Object instance being invoked
     * @param args           the arguments that were passed to the method.
     * @param handleSupplier a (possibly lazy) Handle supplier.
     * @return the method return value, or null if the method has a void return type.
     * @throws Exception any exception thrown by the method.
     */
    Object invoke(Object target, Object[] args, HandleSupplier handleSupplier) throws Exception;

    /**
     * Called after the method handler is constructed to pre-initialize any important
     * configuration data structures.
     *
     * @param config the method configuration to warm
     */
    @Beta
    default void warm(ConfigRegistry config) {}
}

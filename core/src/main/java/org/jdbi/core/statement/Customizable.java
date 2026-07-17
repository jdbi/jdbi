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
package org.jdbi.core.statement;

import org.jdbi.meta.Beta;

/**
 * The surface a statement customizer operates on. It carries parameter binding and attribute defining
 * (from {@link BindingsMixin}) together with the small tail of statement-level operations customizers
 * use: reading the {@link StatementContext}, registering a {@link StatementCustomizer}, setting the
 * query timeout, and attaching to the handle for cleanup.
 *
 * <p>Both the classic {@link SqlStatement} and a {@link QueryTemplateBinding} implement this interface,
 * so a single customizer can be applied to either without knowing which concrete kind of statement it
 * was handed.
 *
 * @param <This> the fluent self type returned by mutating methods
 */
public interface Customizable<This> extends BindingsMixin<This> {

    /**
     * Returns the statement context for this execution.
     *
     * @return the statement context
     */
    StatementContext getContext();

    /**
     * Adds a statement customizer to be applied to this execution's JDBC statement.
     *
     * @param customizer the customizer to add
     * @return this
     */
    This addCustomizer(StatementCustomizer customizer);

    /**
     * Sets the query timeout, in seconds, for this execution.
     *
     * @param seconds number of seconds before timing out
     * @return this
     */
    This setQueryTimeout(int seconds);

    /**
     * Registers this statement's resources to be cleaned up when the owning handle is closed.
     *
     * @return this
     */
    @Beta
    This attachToHandleForCleanup();
}

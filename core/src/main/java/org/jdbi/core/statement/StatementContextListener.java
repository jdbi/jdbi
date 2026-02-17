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
 * Listener interface for the {@link StatementContext}. Each method will be called when specific events with the context happen.
 */
@Beta
public interface StatementContextListener {

    /**
     * A new {@link StatementContext} is created.
     *
     * @param statementContext The {@link StatementContext} object that was created. Never null.
     */
    default void contextCreated(StatementContext statementContext) {}

    /**
     * A {@link StatementContext} object was cleaned. Implementers should be aware that the
     * {@link StatementContext#close()} method can be called multiple times as Statements can be reused.
     *
     * @param statementContext The {@link StatementContext} object that was cleaned. Never null.
     */
    default void contextCleaned(StatementContext statementContext) {}

    /**
     * A {@link Cleanable} object was added to this context for cleanup when the context is cleaned.
     *
     * @param statementContext The {@link StatementContext}. Never null.
     * @param cleanable        The {@link Cleanable} object that should be closed when the context is closed. Never null.
     */
    default void cleanableAdded(StatementContext statementContext, Cleanable cleanable) {}

    /**
     * A {@link Cleanable} object was removed from the context.
     *
     * @param statementContext The {@link StatementContext}. Never null.
     * @param cleanable        The {@link Cleanable} object that was removed from the context.
     */
    default void cleanableRemoved(StatementContext statementContext, Cleanable cleanable) {}
}

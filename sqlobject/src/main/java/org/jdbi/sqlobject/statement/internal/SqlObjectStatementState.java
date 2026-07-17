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
package org.jdbi.sqlobject.statement.internal;

import java.util.function.Supplier;

import org.jdbi.core.statement.StatementContext;

/**
 * Per-invocation state for a SQL Object method call: the method arguments and the producer of the
 * method's return value. It is attached to the execution's {@link StatementContext} (see
 * {@link StatementContext#setExtensionState}) rather than stored on the shared configuration, so that
 * a template binding executed concurrently on many threads never races on it.
 */
final class SqlObjectStatementState {

    private final Object[] args;
    private Supplier<Object> returner;

    SqlObjectStatementState(final Object[] args) {
        this.args = args;
    }

    /**
     * Returns the per-invocation state attached to the given context.
     *
     * @param ctx the execution's statement context
     * @return the attached state
     */
    static SqlObjectStatementState from(final StatementContext ctx) {
        return (SqlObjectStatementState) ctx.getExtensionState();
    }

    Object[] getArgs() {
        return args;
    }

    void setReturner(final Supplier<Object> returner) {
        this.returner = returner;
    }

    Supplier<Object> getReturner() {
        return returner;
    }
}

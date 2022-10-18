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
package org.jdbi.v3.core.statement;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.Collection;

import org.jdbi.v3.core.CloseException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;

abstract class BaseStatement<This> implements Closeable, Configurable<This> {
    @SuppressWarnings("unchecked")
    final This typedThis = (This) this;

    private final Handle handle;
    private final StatementContext ctx;

    BaseStatement(Handle handle) {
        this.handle = handle;
        this.ctx = new StatementContext(handle.getConfig().createCopy(), handle.getExtensionMethod());
    }

    public final Handle getHandle() {
        return handle;
    }

    @Override
    public ConfigRegistry getConfig() {
        return ctx.getConfig();
    }

    /**
     * Returns the statement context associated with this statement.
     *
     * @return the statement context associated with this statement.
     */
    public final StatementContext getContext() {
        return ctx;
    }

    protected final void cleanUpForException(SQLException e) {
        try {
            close();
        } catch (CloseException ce) {
            e.addSuppressed(ce.getCause());
        } catch (Exception e1) {
            e.addSuppressed(e1);
        }
    }

    /**
     * Registers the given {@link Cleanable} to be executed when this statement is closed.
     *
     * @param cleanable the cleanable to register
     * @return this
     */
    This addCleanable(Cleanable cleanable) {
        getContext().addCleanable(cleanable);
        return typedThis;
    }

    void addCustomizers(final Collection<StatementCustomizer> customizers) {
        customizers.forEach(this::addCustomizer);
    }

    final void callCustomizers(StatementCustomizerInvocation invocation) {
        for (StatementCustomizer customizer : getCustomizers()) {
            try {
                invocation.call(customizer);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, ctx);
            }
        }
    }

    private Collection<StatementCustomizer> getCustomizers() {
        return this.getConfig(SqlStatements.class).getCustomizers();
    }

    @Override
    public void close() {
        getContext().close();
    }

    @FunctionalInterface
    interface StatementCustomizerInvocation {
        void call(StatementCustomizer t) throws SQLException;
    }
}

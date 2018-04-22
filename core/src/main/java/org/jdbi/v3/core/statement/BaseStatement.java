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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.generic.GenericTypes;

abstract class BaseStatement<This> implements Closeable, Configurable<This> {
    @SuppressWarnings("unchecked")
    final This typedThis = (This) this;

    private final Handle handle;
    private final StatementContext ctx;
    private final Collection<StatementCustomizer> customizers = new ArrayList<>();

    BaseStatement(Handle handle) {
        this.handle = handle;
        this.ctx = new StatementContext(
                handle.getConfig().createCopy(), handle.getExtensionMethod());
    } {
        // Prevent bogus signatures like Update extends SqlStatement<Query>
        // SqlStatement's generic parameter must be supertype of getClass()
        if (GenericTypes.findGenericParameter(getClass(), BaseStatement.class)
                .map(GenericTypes::getErasedType)
                .map(type -> !type.isAssignableFrom(getClass()))
                .orElse(false)) { // subclass is raw type.. ¯\_(ツ)_/¯
            throw new IllegalStateException("inconsistent SqlStatement hierarchy");
        }
    }

    public Handle getHandle() {
        return handle;
    }

    @Override
    public ConfigRegistry getConfig() {
        return ctx.getConfig();
    }

    /**
     * @return the statement context associated with this statement
     */
    public final StatementContext getContext() {
        return ctx;
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
        this.customizers.addAll(customizers);
    }

    /**
     * Provides a means for custom statement modification. Common customizations
     * have their own methods, such as {@link Query#setMaxRows(int)}
     *
     * @param customizer instance to be used to customize a statement
     * @return this
     */
    public final This addCustomizer(final StatementCustomizer customizer) {
        this.customizers.add(customizer);
        return typedThis;
    }

    final void beforeBinding(final PreparedStatement stmt) {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.beforeBinding(stmt, ctx);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, ctx);
            }
        }
    }

    final void beforeExecution(final PreparedStatement stmt) {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.beforeExecution(stmt, ctx);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, ctx);
            }
        }
    }

    final void afterExecution(final PreparedStatement stmt) {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.afterExecution(stmt, ctx);
            } catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, ctx);
            }
        }
    }

    @Override
    public void close() {
        getContext().close();
    }
}


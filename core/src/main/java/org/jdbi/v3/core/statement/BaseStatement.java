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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.Configurable;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.generic.GenericTypes;

abstract class BaseStatement<This> implements Closeable, Configurable<This>
{
    final This typedThis;

    private final ConfigRegistry config;
    private final StatementBuilder statementBuilder;
    private final StatementContext context;
    private final Collection<StatementCustomizer> customizers = new ArrayList<>();

    @SuppressWarnings("unchecked")
    BaseStatement(ConfigRegistry config, StatementBuilder statementBuilder, StatementContext context)
    {
        assert verifyOurNastyDowncastIsOkay();

        this.typedThis = (This) this;
        this.config = config;
        this.statementBuilder = statementBuilder;
        this.context = context;
    }

    private boolean verifyOurNastyDowncastIsOkay()
    {
        // Prevent bogus signatures like Update extends SqlStatement<Query>
        // SqlStatement's generic parameter must be supertype of getClass()
        return GenericTypes.findGenericParameter(getClass(), BaseStatement.class)
                .map(GenericTypes::getErasedType)
                .map(type -> type.isAssignableFrom(getClass()))
                .orElse(true); // subclass is raw type.. ¯\_(ツ)_/¯
    }

    @Override
    public ConfigRegistry getConfig() {
        return config;
    }

    /**
     * @return the statement context associated with this statement
     */
    public final StatementContext getContext()
    {
        return context;
    }

    protected StatementBuilder getStatementBuilder()
    {
        return statementBuilder;
    }

    /**
     * Registers the given {@link Cleanable} to be executed when this statement is closed.
     *
     * @param cleanable the cleanable to register
     * @return this
     */
    @SuppressWarnings("unchecked")
    This addCleanable(Cleanable cleanable)
    {
        getContext().addCleanable(cleanable);
        return typedThis;
    }

    void addCustomizers(final Collection<StatementCustomizer> customizers)
    {
        this.customizers.addAll(customizers);
    }

    /**
     * Provides a means for custom statement modification. Common customizations
     * have their own methods, such as {@link Query#setMaxRows(int)}
     *
     * @param customizer instance to be used to customize a statement
     * @return this
     */
    @SuppressWarnings("unchecked")
    public final This addCustomizer(final StatementCustomizer customizer)
    {
        this.customizers.add(customizer);
        return typedThis;
    }

    final void beforeExecution(final PreparedStatement stmt)
    {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.beforeExecution(stmt, context);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, context);
            }
        }
    }

    final void afterExecution(final PreparedStatement stmt)
    {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.afterExecution(stmt, context);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Exception thrown in statement customization", e, context);
            }
        }
    }

    @Override
    public void close()
    {
        getContext().close();
    }
}


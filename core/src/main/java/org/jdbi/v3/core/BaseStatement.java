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
package org.jdbi.v3.core;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jdbi.v3.core.Cleanables.Cleanable;
import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.statement.StatementCustomizer;

abstract class BaseStatement
{
    final JdbiConfig config;
    private final Collection<StatementCustomizer> customizers = new ArrayList<>();
    private final ConcreteStatementContext context;

    BaseStatement(JdbiConfig config, ConcreteStatementContext context)
    {
        this.config = config;
        this.context = context;
        addCustomizer(new StatementCleaningCustomizer());
    }

    final ArgumentRegistry getArgumentRegistry() {
        return config.argumentRegistry;
    }

    /**
     * @return the statement context associated with this statement
     */
    public final ConcreteStatementContext getContext()
    {
        return context;
    }

    protected void addCustomizers(final Collection<StatementCustomizer> customizers)
    {
        this.customizers.addAll(customizers);
    }

    protected void addCustomizer(final StatementCustomizer customizer)
    {
        this.customizers.add(customizer);
    }

    protected Collection<StatementCustomizer> getStatementCustomizers()
    {
        return this.customizers;
    }

    protected final void beforeExecution(final PreparedStatement stmt)
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

    protected final void afterExecution(final PreparedStatement stmt)
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

    protected final void cleanup()
    {
        for (StatementCustomizer customizer : customizers) {
            try {
                customizer.cleanup(context);
            }
            catch (SQLException e) {
                throw new UnableToExecuteStatementException("Could not clean up", e, context);
            }
        }
    }

    protected void addCleanable(final Cleanable cleanable)
    {
        context.getCleanables().add(cleanable);
    }

    class StatementCleaningCustomizer implements StatementCustomizer
    {
        @Override
        public final void cleanup(final StatementContext ctx)
            throws SQLException
        {
            context.getCleanables().clean();
        }
    }
}


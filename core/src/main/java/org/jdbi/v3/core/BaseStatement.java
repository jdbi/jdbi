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

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.jdbi.v3.core.exception.UnableToExecuteStatementException;
import org.jdbi.v3.core.statement.StatementCustomizer;

abstract class BaseStatement<This> implements Closeable, Configurable<This>
{
    private final ConfigRegistry config;
    private final StatementContext context;
    private final Collection<StatementCustomizer> customizers = new ArrayList<>();

    BaseStatement(ConfigRegistry config, StatementContext context)
    {
        this.config = config;
        this.context = context;
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

    void addCleanable(Cleanable cleanable)
    {
        getContext().getCleanables().add(cleanable);
    }

    void addCustomizers(final Collection<StatementCustomizer> customizers)
    {
        this.customizers.addAll(customizers);
    }

    void addCustomizer(final StatementCustomizer customizer)
    {
        this.customizers.add(customizer);
    }

    Collection<StatementCustomizer> getStatementCustomizers()
    {
        return this.customizers;
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


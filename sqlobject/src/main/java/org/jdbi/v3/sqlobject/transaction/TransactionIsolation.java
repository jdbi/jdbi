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
package org.jdbi.v3.sqlobject.transaction;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.SqlStatement;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementCustomizer;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.sqlobject.customizer.SqlStatementParameterCustomizer;

/**
 * Used to specify the transaction isolation level for an object or method (via annotating the method
 * or passing it in as an annotated param). If used on a parameter, the parameter type must be a
 * {@link TransactionIsolationLevel}
 */
@SqlStatementCustomizingAnnotation(TransactionIsolation.Factory.class)
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TransactionIsolation
{

    TransactionIsolationLevel value() default TransactionIsolationLevel.INVALID_LEVEL;

    class Factory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return new MyCustomizer(((TransactionIsolation) annotation).value());
        }

        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
        {
            return new MyCustomizer(((TransactionIsolation) annotation).value());
        }

        @Override
        public SqlStatementParameterCustomizer createForParameter(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method, Parameter param, int index)
        {
            return (stmt, arg) -> new MyCustomizer((TransactionIsolationLevel) arg).apply(stmt);
        }
    }

    class MyCustomizer implements SqlStatementCustomizer
    {

        private final TransactionIsolationLevel level;

        MyCustomizer(TransactionIsolationLevel level) { this.level = level; }

        @Override
        public void apply(SqlStatement<?> q) throws SQLException
        {
            final int initial_level = q.getContext().getConnection().getTransactionIsolation();

            q.addCustomizer(new StatementCustomizer()
            {
                @Override
                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    setTxnIsolation(ctx, level.intValue());
                }

                @Override
                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    setTxnIsolation(ctx, initial_level);
                }

                private void setTxnIsolation(StatementContext ctx, int level) throws SQLException
                {
                    final Connection c = ctx.getConnection();
                    if (c.getTransactionIsolation() != level) {
                        c.setTransactionIsolation(level);
                    }
                }
            });
        }
    }
}

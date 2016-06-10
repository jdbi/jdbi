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
package org.jdbi.v3.sqlobject.customizers;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jdbi.v3.SqlStatement;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.statement.StatementCustomizer;
import org.jdbi.v3.transaction.TransactionIsolationLevel;

/**
 * Used to specify the transaction isolation level for an object or method (via annotating the method
 * or passing it in as an annotated param). If used on a parameter, the parameter type must be a
 * {@link org.jdbi.v3.transaction.TransactionIsolationLevel}
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
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            return new MyCustomizer(((TransactionIsolation) annotation).value());
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            return new MyCustomizer(((TransactionIsolation) annotation).value());
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Object arg)
        {
            assert arg instanceof TransactionIsolationLevel;
            return new MyCustomizer((TransactionIsolationLevel) arg);
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

            q.addStatementCustomizer(new StatementCustomizer()
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

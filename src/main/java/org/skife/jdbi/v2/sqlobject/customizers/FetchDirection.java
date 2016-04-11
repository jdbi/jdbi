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
package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementCustomizers;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * Used to specify the fetch direction, per JDBC, of a result set.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@SqlStatementCustomizingAnnotation(FetchDirection.Factory.class)
public @interface FetchDirection
{
    /**
     * Set to a value valid for fetch direction on the jdc statement
     */
    int value();

    class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForParameter(Annotation annotation, Object arg)
        {
            return new FetchDirectionSqlStatementCustomizer((Integer) arg);
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new FetchDirectionSqlStatementCustomizer(fs.value());
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new FetchDirectionSqlStatementCustomizer(fs.value());
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
                                                         Method method,
                                                         Object arg)
        {
            return new FetchDirectionSqlStatementCustomizer((Integer) arg);
        }
    }

    class FetchDirectionSqlStatementCustomizer implements SqlStatementCustomizer
    {
        private final Integer direction;

        FetchDirectionSqlStatementCustomizer(final Integer direction)
        {
            this.direction = direction;
        }

        @Override
        public void apply(SQLStatement q) throws SQLException
        {
            q.addStatementCustomizer(new StatementCustomizers.FetchDirectionStatementCustomizer(direction));
        }
    }
}

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
 * Specify the query timeout in seconds. May be used on a method or parameter, the parameter must be of an int type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(QueryTimeOut.Factory.class)
public @interface QueryTimeOut
{
    int value() default Integer.MAX_VALUE;

    static class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final QueryTimeOut fs = (QueryTimeOut) annotation;
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q) throws SQLException
                {
                    q.setQueryTimeout(fs.value());
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final QueryTimeOut fs = (QueryTimeOut) annotation;
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q) throws SQLException
                {
                    q.setQueryTimeout(fs.value());
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
                                                         Method method,
                                                         Object arg)
        {
            final Integer va = (Integer) arg;
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q) throws SQLException
                {
                    q.setQueryTimeout(va);
                }
            };
        }
    }


}

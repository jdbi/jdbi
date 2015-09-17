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

import org.skife.jdbi.v2.Query;
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
 * Used to specify the maximum numb er of rows to return on a result set. Passes through to
 * setMaxRows on the JDBC prepared statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(MaxRows.Factory.class)
public @interface MaxRows
{
    /**
     * The max number of rows to return from the query.
     */
    int value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final int va = ((MaxRows)annotation).value();
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query)q).setMaxRows(va);
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final int va = ((MaxRows)annotation).value();
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query)q).setMaxRows(va);
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            final Integer va = (Integer) arg;
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query)q).setMaxRows(va);
                }
            };
        }
    }


}

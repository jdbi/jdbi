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
package org.jdbi.v3.sqlobject.customizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.Query;

/**
 * Used to specify the maximum number of rows to return on a result set. Passes through to
 * setMaxRows on the JDBC prepared statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(MaxRows.Factory.class)
public @interface MaxRows
{
    /**
     * The max number of rows to return from the query.
     * @return the max rows
     */
    int value();

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            final int va = ((MaxRows)annotation).value();
            return q -> {
                assert q instanceof Query;
                ((Query)q).setMaxRows(va);
            };
        }

        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
        {
            final int va = ((MaxRows)annotation).value();
            return q -> {
                assert q instanceof Query;
                ((Query)q).setMaxRows(va);
            };
        }

        @Override
        public SqlStatementParameterCustomizer createForParameter(ConfigRegistry registry,
                                                         Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Parameter param,
                                                         int index)
        {
            return (stmt, arg) -> {
                ((Query)stmt).setMaxRows((Integer) arg);
            };
        }
    }
}

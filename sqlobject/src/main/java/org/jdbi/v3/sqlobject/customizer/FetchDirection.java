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
import org.jdbi.v3.core.statement.StatementCustomizers;

/**
 * Used to specify the fetch direction, per JDBC, of a result set.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@SqlStatementCustomizingAnnotation(FetchDirection.Factory.class)
public @interface FetchDirection
{
    /**
     * Set to a value valid for fetch direction on the jdbc statement
     * @return the fetch direction
     */
    int value();

    class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForParameter(Annotation annotation, Object arg)
        {
            return create((Integer) arg);
        }

        @Override
        public SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return create(fs.value());
        }

        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return create(fs.value());
        }

        @Override
        public SqlStatementParameterCustomizer createForParameter(ConfigRegistry registry,
                                                         Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Parameter param,
                                                         int index)
        {
            return (stmt, arg) -> create((Integer) arg).apply(stmt);
        }

        private static SqlStatementCustomizer create(Integer direction)
        {
            return q -> q.addCustomizer(new StatementCustomizers.FetchDirectionStatementCustomizer(direction));
        }
    }
}

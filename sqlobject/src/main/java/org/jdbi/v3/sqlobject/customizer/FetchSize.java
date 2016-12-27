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

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(FetchSize.Factory.class)
public @interface FetchSize
{
    int value() default 0;

    class Factory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForMethod(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            final FetchSize fs = (FetchSize) annotation;
            return q -> {
                assert q instanceof Query;
                ((Query) q).setFetchSize(fs.value());
            };
        }

        @Override
        public SqlStatementCustomizer createForType(ConfigRegistry registry, Annotation annotation, Class<?> sqlObjectType)
        {
            final FetchSize fs = (FetchSize) annotation;
            return q -> {
                assert q instanceof Query;
                ((Query) q).setFetchSize(fs.value());
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
                ((Query) stmt).setFetchSize((Integer) arg);
            };
        }
    }
}

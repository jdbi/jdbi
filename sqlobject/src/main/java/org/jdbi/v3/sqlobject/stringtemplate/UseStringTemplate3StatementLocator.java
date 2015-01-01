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
package org.jdbi.v3.sqlobject.stringtemplate;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.jdbi.v3.SQLStatement;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.tweak.StatementLocator;

@SqlStatementCustomizingAnnotation(UseStringTemplate3StatementLocator.LocatorFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface UseStringTemplate3StatementLocator
{
    String DEFAULT_VALUE = " ~ ";

    String value() default DEFAULT_VALUE;

    public static class LocatorFactory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            final UseStringTemplate3StatementLocator a = (UseStringTemplate3StatementLocator) annotation;
            final StringTemplate3StatementLocator.Builder builder;

            if (DEFAULT_VALUE.equals(a.value())) {
                builder = StringTemplate3StatementLocator.builder(sqlObjectType);
            }
            else {
                builder = StringTemplate3StatementLocator.builder(a.value());
            }

            final StatementLocator l = builder.allowImplicitTemplateGroup().treatLiteralsAsTemplates().shouldCache().build();

            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement<?> q)
                {
                    q.setStatementLocator(l);
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation,
                                                      Class<?> sqlObjectType,
                                                      Method method)
        {
            throw new UnsupportedOperationException("Not Defined on Method");
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not defined on parameter");
        }
    }

}

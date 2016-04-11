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
package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * @deprecated use {@link UseStringTemplate3StatementLocator}
 */
@Deprecated
@SqlStatementCustomizingAnnotation(ExternalizedSqlViaStringTemplate3.LocatorFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExternalizedSqlViaStringTemplate3
{
    String DEFAULT_VALUE = " ~ ";

    String value() default DEFAULT_VALUE;

    class LocatorFactory implements SqlStatementCustomizerFactory
    {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final ExternalizedSqlViaStringTemplate3 a = (ExternalizedSqlViaStringTemplate3) annotation;
            final StringTemplate3StatementLocator.Builder builder;
            if (DEFAULT_VALUE.equals(a.value())) {
                builder = StringTemplate3StatementLocator.builder(sqlObjectType);
            }
            else {
                builder = StringTemplate3StatementLocator.builder(a.value());
            }

            final StatementLocator l = builder.shouldCache().build();

            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(l);
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation,
                                                      Class sqlObjectType,
                                                      Method method)
        {
            throw new UnsupportedOperationException("Not Defined on Method");
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
                                                         Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not defined on parameter");
        }
    }

}

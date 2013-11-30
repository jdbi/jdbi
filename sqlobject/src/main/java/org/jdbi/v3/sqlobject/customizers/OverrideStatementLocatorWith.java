/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.jdbi.v3.SQLStatement;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;
import org.jdbi.v3.tweak.StatementLocator;

/**
 * Use this to override the statement locator on a sql object, May be specified on either the interface
 * or method level.
 */
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(OverrideStatementLocatorWith.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OverrideStatementLocatorWith
{
    /**
     * Specify the statement locator class to use
     */
    Class<? extends org.jdbi.v3.tweak.StatementLocator> value();

    static class Factory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            OverrideStatementLocatorWith sl = (OverrideStatementLocatorWith) annotation;
            final org.jdbi.v3.tweak.StatementLocator f;
            try {
                f = instantiate(sl.value(), sqlObjectType, method);
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to instantiate a statement locator", e);
            }
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement<?> q)
                {
                    q.setStatementLocator(f);
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            OverrideStatementLocatorWith sl = (OverrideStatementLocatorWith) annotation;
            final StatementLocator f;
            try {
                f = instantiate(sl.value(), sqlObjectType, null);
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to instantiate a statement locator", e);
            }
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement<?> q)
                {
                    q.setStatementLocator(f);
                }
            };
        }


        private StatementLocator instantiate(Class<? extends StatementLocator> value,
                                             Class<?> sqlObjectType,
                                             Method m) throws Exception
        {
            try {
                Constructor<? extends StatementLocator> no_arg = value.getConstructor();
                return no_arg.newInstance();
            }
            catch (NoSuchMethodException e) {
                try {
                    Constructor<? extends StatementLocator> class_arg = value.getConstructor(Class.class);
                    return class_arg.newInstance(sqlObjectType);
                }
                catch (NoSuchMethodException e1) {
                    if (m != null) {
                        Constructor<? extends StatementLocator> c_m_arg = value.getConstructor(Class.class,
                                                                                               Method.class);
                        return c_m_arg.newInstance(sqlObjectType, m);
                    }
                    throw new IllegalStateException("Unable to instantiate, no viable constructor " + value.getName());
                }
            }

        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class<?> sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not applicable to parameter");
        }
    }
}

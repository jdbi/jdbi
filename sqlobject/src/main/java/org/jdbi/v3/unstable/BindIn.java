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
package org.jdbi.v3.unstable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.SQLStatement;
import org.jdbi.v3.sqlobject.Binder;
import org.jdbi.v3.sqlobject.BinderFactory;
import org.jdbi.v3.sqlobject.BindingAnnotation;
import org.jdbi.v3.sqlobject.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.SqlStatementCustomizingAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(BindIn.CustomizerFactory.class)
@BindingAnnotation(BindIn.BindingFactory.class)
public @interface BindIn
{
    String value();

    public static final class CustomizerFactory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class<?> sqlObjectType, Method method)
        {
            throw new UnsupportedOperationException("Not supported on method!");
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType)
        {
            throw new UnsupportedOperationException("Not supported on type");
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class<?> sqlObjectType,
                                                         Method method,
                                                         Object arg)
        {
            Collection<?> coll = (Collection<?>) arg;
            BindIn in = (BindIn) annotation;
            final String key = in.value();
            final List<String> ids = new ArrayList<String>();
            for (int idx = 0; idx < coll.size(); idx++) {
                ids.add("__" + key + "_" + idx);
            }

            StringBuilder names = new StringBuilder();
            for (Iterator<String> i = ids.iterator(); i.hasNext();) {
                names.append(":").append(i.next());
                if (i.hasNext()) {
                    names.append(",");
                }
            }
            final String ns = names.toString();

            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement<?> q) throws SQLException
                {
                    q.define(key, ns);
                }
            };
        }
    }

    public static class BindingFactory implements BinderFactory
    {
        @Override
        public Binder<?,?> build(Annotation annotation)
        {
            final BindIn in = (BindIn) annotation;
            final String key = in.value();

            return new Binder<Annotation, Object>()
            {

                @Override
                public void bind(SQLStatement<?> q, Parameter param, Annotation bind, Object arg)
                {
                    Iterable<?> coll = (Iterable<?>) arg;
                    int idx = 0;
                    for (Object s : coll) {
                        q.bind("__" + key + "_" + idx++, s);
                    }
                }
            };
        }
    }
}

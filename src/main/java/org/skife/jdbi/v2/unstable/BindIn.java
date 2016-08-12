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
package org.skife.jdbi.v2.unstable;

import org.skife.jdbi.v2.ClasspathStatementLocator;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator.LocatorFactory;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocatorImpl;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(BindIn.CustomizerFactory.class)
@BindingAnnotation(BindIn.BindingFactory.class)
public @interface BindIn
{
    String value();

    final class CustomizerFactory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            throw new UnsupportedOperationException("Not supported on method!");
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            throw new UnsupportedOperationException("Not supported on type");
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         final Class sqlObjectType,
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
                public void apply(SQLStatement q) throws SQLException
                {
                    q.define(key, ns);
                    if (q.getStatementLocator() instanceof ClasspathStatementLocator) {
                        new LocatorFactory().createForType(UseStringTemplate3StatementLocatorImpl.defaultInstance(), sqlObjectType).apply(q);
                    }
                }
            };
        }
    }

    class BindingFactory implements BinderFactory<BindIn>
    {
        @Override
        public Binder build(BindIn in)
        {
            final String key = in.value();

            return new Binder<Annotation, Iterable>()
            {
                @Override
                public void bind(SQLStatement q, Annotation bind, Iterable coll)
                {
                    int idx = 0;
                    for (Object s : coll) {
                        q.bind("__" + key + "_" + idx++, s);
                    }
                }
            };
        }
    }
}

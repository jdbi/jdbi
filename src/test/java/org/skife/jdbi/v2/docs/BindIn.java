package org.skife.jdbi.v2.docs;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;

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

    public static final class CustomizerFactory implements SqlStatementCustomizerFactory
    {

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            throw new UnsupportedOperationException("Not supported on method!");
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            throw new UnsupportedOperationException("Not supported on type");
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
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
                public void apply(SQLStatement q) throws SQLException
                {
                    q.define(key, ns);
                }
            };
        }
    }

    public static class BindingFactory implements BinderFactory
    {
        public Binder build(Annotation annotation)
        {
            final BindIn in = (BindIn) annotation;
            final String key = in.value();

            return new Binder()
            {

                public void bind(SQLStatement q, Annotation bind, Object arg)
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

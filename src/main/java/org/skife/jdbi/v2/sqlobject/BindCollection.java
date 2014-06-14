package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(BindCollection.CustomizerFactory.class)
@BindingAnnotation(BindCollection.BindingFactory.class)
public @interface BindCollection
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
            final Collection<?> coll = (Collection<?>) arg;

            BindCollection in = (BindCollection) annotation;
            final String key = in.value();

            final List<String> placeholdersToBeBoundByJdbi = new ArrayList<String>();
            for (int idx = 0; idx < coll.size(); idx++) {
                placeholdersToBeBoundByJdbi.add(":" + placeholderFor(key, idx));
            }

            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    q.define(key, placeholdersToBeBoundByJdbi);
                }
            };
        }

        static String placeholderFor(String key, Integer index)
        {
            return "__" + key + "_" + index;
        }
    }

    public static class BindingFactory implements BinderFactory
    {

        public Binder build(Annotation annotation)
        {
            final BindCollection in = (BindCollection) annotation;
            final String key = in.value();

            return new Binder()
            {

                public void bind(SQLStatement q, Annotation bind, Object arg)
                {
                    Iterable<?> coll = (Iterable<?>) arg;
                    int idx = 0;
                    for (Object value : coll) {
                        String placeholder = CustomizerFactory.placeholderFor(key, idx++);
                        bindBothRawValueAndBeanProperties(q, placeholder, value);
                    }
                }

                private void bindBothRawValueAndBeanProperties(SQLStatement q, String placeholder, Object value)
                {
                    q.bind(placeholder, value);
                    BindBeanFactory.bindBeanProperties(q, placeholder, value);
                }
            };
        }
    }
}

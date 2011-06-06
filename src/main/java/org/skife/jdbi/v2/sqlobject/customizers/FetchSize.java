package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(FetchSize.Factory.class)
public @interface FetchSize
{
    int value() default 0;

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final FetchSize fs = (FetchSize) annotation;
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query) q).setFetchSize(fs.value());
                }
            };
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final FetchSize fs = (FetchSize) annotation;
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query) q).setFetchSize(fs.value());
                }
            };
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            final Integer va = (Integer) arg;
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query) q).setFetchSize(va);
                }
            };
        }
    }
}

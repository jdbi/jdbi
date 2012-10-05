package org.skife.jdbi.v2.sqlobject.helpers;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.BeanMapperFactory;
import org.skife.jdbi.v2.util.NamingStrategy;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;

@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(MapResultAsBean.MapAsBeanFactory.class)
@Target(ElementType.METHOD)
public @interface MapResultAsBean
{
    NamingStrategy dbFormattingStrategy() default NamingStrategy.LOWER;

    NamingStrategy propertyFormattingStrategy() default NamingStrategy.LOWER;

    public static class MapAsBeanFactory implements SqlStatementCustomizerFactory
    {

        @Override
        public SqlStatementCustomizer createForMethod(final Annotation annotation, Class sqlObjectType, Method method)
        {
            return new SqlStatementCustomizer()
            {
                @Override
                public void apply(SQLStatement s) throws SQLException
                {
                   Query q = (Query) s;
                   MapResultAsBean asBean = (MapResultAsBean) annotation;
                   q.registerMapper(new BeanMapperFactory(asBean.dbFormattingStrategy(),asBean.propertyFormattingStrategy()));
                }
            };
        }

        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            throw new UnsupportedOperationException("Not allowed on type");
        }

        @Override
        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not allowed on parameter");
        }
    }
}

package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.ContainerFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SqlStatementCustomizingAnnotation(RegisterContainerMapper.Factory.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterContainerMapper
{
    Class<? extends ContainerFactory>[] value();

    public static class Factory implements SqlStatementCustomizerFactory
    {

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            return new MyCustomizer((RegisterContainerMapper) annotation);
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            return new MyCustomizer((RegisterContainerMapper) annotation);
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not Yet Implemented!");
        }
    }

    static class MyCustomizer implements SqlStatementCustomizer
    {
        private final List<ContainerFactory> factory;

        MyCustomizer(RegisterContainerMapper annotation)
        {
            List<ContainerFactory> ls = new ArrayList<ContainerFactory>();
            try {
                for (Class<? extends ContainerFactory> type : annotation.value()) {
                    ls.add(type.newInstance());
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("Unable to instantiate container factory", e);
            }
            this.factory = ls;
        }

        public void apply(SQLStatement q) throws SQLException
        {
            if (q instanceof Query) {
                Query query = (Query) q;
                for (ContainerFactory containerFactory : factory) {
                    query.registerContainerFactory(containerFactory);
                }
            }

        }
    }
}

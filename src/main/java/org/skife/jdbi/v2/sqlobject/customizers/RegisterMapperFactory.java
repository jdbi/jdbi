package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * Used to register a result set mapper with either a sql object type or for a specific method.
 */
@SqlStatementCustomizingAnnotation(RegisterMapperFactory.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterMapperFactory
{

    Class<? extends ResultSetMapperFactory>[] value();

    public static class Factory implements SqlStatementCustomizerFactory
    {

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final RegisterMapperFactory ma = (RegisterMapperFactory) annotation;
            final ResultSetMapperFactory[] m = new ResultSetMapperFactory[ma.value().length];
            try {
                Class<? extends ResultSetMapperFactory>[] mcs = ma.value();
                for (int i = 0; i < mcs.length; i++) {
                    m[i] = mcs[i].newInstance();
                }

            }
            catch (Exception e) {
                throw new IllegalStateException("unable to create a specified result set mapper", e);
            }
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement statement)
                {
                    if (statement instanceof Query) {
                        Query q = (Query) statement;
                        for (ResultSetMapperFactory factory : m) {
                            q.registerMapper(factory);
                        }

                    }
                }
            };
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final RegisterMapperFactory ma = (RegisterMapperFactory) annotation;
            final ResultSetMapperFactory[] m = new ResultSetMapperFactory[ma.value().length];
            try {
                Class<? extends ResultSetMapperFactory>[] mcs = ma.value();
                for (int i = 0; i < mcs.length; i++) {
                    m[i] = mcs[i].newInstance();
                }

            }
            catch (Exception e) {
                throw new IllegalStateException("unable to create a specified result set mapper", e);
            }
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement statement)
                {
                    if (statement instanceof Query) {
                        Query q = (Query) statement;
                        for (ResultSetMapperFactory factory : m) {
                            q.registerMapper(factory);
                        }

                    }
                }
            };
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not defined for parameter");
        }
    }
}

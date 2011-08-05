package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * Used to register a result set mapper with either a sql object type or for a specific method.
 */
@SqlStatementCustomizingAnnotation(RegisterMapper.Factory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RegisterMapper
{
    /**
     * The result set mapper class to register
     */
    Class<? extends ResultSetMapper<?>>[] value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final RegisterMapper ma = (RegisterMapper) annotation;
            final ResultSetMapper[] m = new ResultSetMapper[ma.value().length];
            try {
                Class<? extends ResultSetMapper<?>>[] mcs = ma.value();
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
                        for (ResultSetMapper mapper : m) {
                            q.registerMapper(mapper);
                        }

                    }
                }
            };
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final RegisterMapper ma = (RegisterMapper) annotation;
            final ResultSetMapper[] m = new ResultSetMapper[ma.value().length];
            try {
                Class<? extends ResultSetMapper<?>>[] mcs = ma.value();
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
                        for (ResultSetMapper mapper : m) {
                            q.registerMapper(mapper);
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

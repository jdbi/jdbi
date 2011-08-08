package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementCustomizers;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * Used to specify the fetch direction, per JDBC, of a result set.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@SqlStatementCustomizingAnnotation(FetchDirection.Factory.class)
public @interface FetchDirection
{
    /**
     * Set to a value valid for fetch direction on the jdc statement
     */
    int value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForParameter(Annotation annotation, Object arg)
        {
            return new FetchDirectionSqlStatementCustomizer((Integer) arg);
        }

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new FetchDirectionSqlStatementCustomizer(fs.value());
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new FetchDirectionSqlStatementCustomizer(fs.value());
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
                                                         Method method,
                                                         Object arg)
        {
            return new FetchDirectionSqlStatementCustomizer((Integer) arg);
        }
    }

    static class FetchDirectionSqlStatementCustomizer implements SqlStatementCustomizer
    {
        private final Integer direction;

        public FetchDirectionSqlStatementCustomizer(final Integer direction)
        {
            this.direction = direction;
        }

        public void apply(SQLStatement q) throws SQLException
        {
            q.addStatementCustomizer(new StatementCustomizers.FetchDirectionStatementCustomizer(direction));
        }
    }
}

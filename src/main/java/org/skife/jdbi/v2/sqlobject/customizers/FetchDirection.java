package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
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
        public StatementCustomizer createForParameter(Annotation annotation, Object arg)
        {
            final Integer va = (Integer) arg;
            return new StatementCustomizer() {

                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    stmt.setFetchDirection(va);
                }

                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                }
            };
        }

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new SqlStatementCustomizer() {
                public void apply(SQLStatement q) throws SQLException
                {
                    q.setFetchDirection(fs.value());
                }
            };
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new SqlStatementCustomizer() {
                public void apply(SQLStatement q) throws SQLException
                {
                    q.setFetchDirection(fs.value());
                }
            };
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
                                                         Method method,
                                                         Object arg)
        {
            final Integer va = (Integer) arg;
            return new SqlStatementCustomizer() {
                public void apply(SQLStatement q) throws SQLException
                {
                    q.setFetchDirection(va);
                }
            };
        }
    }


}

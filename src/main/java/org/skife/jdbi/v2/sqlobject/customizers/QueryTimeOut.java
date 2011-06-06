package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.CustomizerAnnotation;
import org.skife.jdbi.v2.sqlobject.JDBCStatementCustomizerFactory;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Specify the query timeout in seconds. May be used on a method or parameter, the parameter must be of an int type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@CustomizerAnnotation(QueryTimeOut.Factory.class)
public @interface QueryTimeOut
{
    int value() default Integer.MAX_VALUE;

    static class Factory implements JDBCStatementCustomizerFactory
    {
        public StatementCustomizer createForParameter(Annotation annotation, Object arg)
        {
            final Integer va = (Integer) arg;
            return new StatementCustomizer() {

                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    stmt.setQueryTimeout(va);
                }

                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                }
            };
        }

        public StatementCustomizer createForMethod(Annotation annotation)
        {
            final QueryTimeOut fs = (QueryTimeOut) annotation;
            return new StatementCustomizer() {

                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    stmt.setQueryTimeout(fs.value());
                }

                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                }
            };
        }
    }


}

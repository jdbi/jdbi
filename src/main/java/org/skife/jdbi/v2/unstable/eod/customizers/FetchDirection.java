package org.skife.jdbi.v2.unstable.eod.customizers;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.StatementCustomizer;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Customizer(FetchDirection.Factory.class)
public @interface FetchDirection
{
    int value() default Integer.MAX_VALUE;

    static class Factory implements StatementCustomizerFactory
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

        public StatementCustomizer createForMethod(Annotation annotation)
        {
            final FetchDirection fs = (FetchDirection) annotation;
            return new StatementCustomizer() {

                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    stmt.setFetchDirection(fs.value());
                }

                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                }
            };
        }
    }


}

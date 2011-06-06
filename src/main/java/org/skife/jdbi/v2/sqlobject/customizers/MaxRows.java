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
 * Used to specify the maximum numb er of rows to return on a result set. Passes through to
 * setMaxRows on the JDBC prepared statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@CustomizerAnnotation(MaxRows.Factory.class)
public @interface MaxRows
{
    /**
     * The max number of rows to return from the query.
     */
    int value();

    static class Factory implements JDBCStatementCustomizerFactory
    {
        public StatementCustomizer createForParameter(Annotation annotation, Object arg)
        {
            final Integer va = (Integer) arg;
            return new StatementCustomizer() {

                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    stmt.setMaxRows(va);
                }

                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                }
            };
        }

        public StatementCustomizer createForMethod(Annotation annotation)
        {
            final MaxRows fs = (MaxRows) annotation;
            return new StatementCustomizer() {

                public void beforeExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                    stmt.setMaxRows(fs.value());
                }

                public void afterExecution(PreparedStatement stmt, StatementContext ctx) throws SQLException
                {
                }
            };
        }
    }


}

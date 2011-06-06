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

/**
 * Used to specify the maximum numb er of rows to return on a result set. Passes through to
 * setMaxRows on the JDBC prepared statement.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER})
@SqlStatementCustomizingAnnotation(MaxRows.Factory.class)
public @interface MaxRows
{
    /**
     * The max number of rows to return from the query.
     */
    int value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            final int va = ((MaxRows)annotation).value();
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query)q).setMaxRows(va);
                }
            };
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final int va = ((MaxRows)annotation).value();
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    assert q instanceof Query;
                    ((Query)q).setMaxRows(va);
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
                    ((Query)q).setMaxRows(va);
                }
            };
        }
    }


}

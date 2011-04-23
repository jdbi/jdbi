package org.skife.jdbi.v2.sqlobject.stringtemplate;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SQLStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SQLStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SQLStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@SQLStatementCustomizingAnnotation(ExternalizedSqlViaStringTemplate3.LocatorFactory.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ExternalizedSqlViaStringTemplate3
{
    static final String DEFAULT_VALUE = " ~ ";

    String value() default DEFAULT_VALUE;

    public static class LocatorFactory implements SQLStatementCustomizerFactory
    {
        public SQLStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            final ExternalizedSqlViaStringTemplate3 a = (ExternalizedSqlViaStringTemplate3) annotation;
            final StatementLocator l;
            if (DEFAULT_VALUE.equals(a.value())) {
                l = new StringTemplate3StatementLocator(sqlObjectType);
            }
            else {
                l = new StringTemplate3StatementLocator(a.value());
            }

            return new SQLStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(l);
                }
            };
        }

        public SQLStatementCustomizer createForMethod(Annotation annotation,
                                                      Class sqlObjectType,
                                                      Method method)
        {
            throw new UnsupportedOperationException("Not Defined on Method");
        }

        public SQLStatementCustomizer createForParameter(Annotation annotation,
                                                         Class sqlObjectType,
                                                         Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not defined on parameter");
        }
    }

}

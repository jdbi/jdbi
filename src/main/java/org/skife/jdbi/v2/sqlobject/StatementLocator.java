package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

@Retention(RetentionPolicy.RUNTIME)
@SQLStatementCustomizingAnnotation(StatementLocator.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface StatementLocator
{
    Class<? extends org.skife.jdbi.v2.tweak.StatementLocator> value();

    static class Factory implements SQLStatementCustomizerFactory {

        public SQLStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            StatementLocator sl = (StatementLocator) annotation;
            final org.skife.jdbi.v2.tweak.StatementLocator f;
            try {
                f = sl.value().newInstance();
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to instantiate a statement locator", e);
            }
            return new SQLStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(f);
                }
            };
        }

        public SQLStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            StatementLocator sl = (StatementLocator) annotation;
            final org.skife.jdbi.v2.tweak.StatementLocator f;
            try {
                f = sl.value().newInstance();
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to instantiate a statement locator", e);
            }
            return new SQLStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(f);
                }
            };
        }

        public SQLStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not applicable to parameter");
        }
    }
}

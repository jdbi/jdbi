package org.skife.jdbi.v2.sqlobject.customizers;

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

/**
 * Used to set attributes on the StatementContext for the statement generated for this method.
 * These values will be available to other customizers, such as the statement locator or rewriter.
 */
@SqlStatementCustomizingAnnotation(Define.Factory.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Define
{
    /**
     * The key for the attribute to set. The value will be the value passed to the annotated argument
     */
    String value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            throw new UnsupportedOperationException("Not allowed on Type");
        }

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            throw new UnsupportedOperationException("Not allowed on Method");
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, final Object arg)
        {
            Define d = (Define) annotation;
            final String key = d.value();
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.define(key,  arg);
                }
            };
        }
    }
}

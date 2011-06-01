package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

/**
 * @deprecated use {@link org.skife.jdbi.v2.sqlobject.customizers.Define} instead
 */
@Deprecated()
@SQLStatementCustomizingAnnotation(Define.Factory.class)
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Define
{
    /**
     * The key for the attribute to set. The value will be the value passed to the annotated argument
     */
    String value();

    static class Factory implements SQLStatementCustomizerFactory
    {
        public SQLStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            throw new UnsupportedOperationException("Not allowed on Type");
        }

        public SQLStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            throw new UnsupportedOperationException("Not allowed on Method");
        }

        public SQLStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, final Object arg)
        {
            Define d = (Define) annotation;
            final String key = d.value();
            return new SQLStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.define(key,  arg);
                }
            };
        }
    }
}

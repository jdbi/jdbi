package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.tweak.StatementLocator;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @deprecated use {@link org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementLocatorWith} instead
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(OverrideStatementLocatorWith.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface OverrideStatementLocatorWith
{
    Class<? extends org.skife.jdbi.v2.tweak.StatementLocator> value();

    /**
     * @deprecated
     */
    @Deprecated
    static class Factory implements SqlStatementCustomizerFactory
    {

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            OverrideStatementLocatorWith sl = (OverrideStatementLocatorWith) annotation;
            final org.skife.jdbi.v2.tweak.StatementLocator f;
            try {
                f = instantiate(sl.value(), sqlObjectType, method);
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to instantiate a statement locator", e);
            }
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(f);
                }
            };
        }

        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            OverrideStatementLocatorWith sl = (OverrideStatementLocatorWith) annotation;
            final StatementLocator f;
            try {
                f = instantiate(sl.value(), sqlObjectType, null);
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to instantiate a statement locator", e);
            }
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q)
                {
                    q.setStatementLocator(f);
                }
            };
        }


        private StatementLocator instantiate(Class<? extends StatementLocator> value,
                                             Class sqlObjectType,
                                             Method m) throws Exception
        {
            try {
                Constructor<? extends StatementLocator> no_arg = value.getConstructor();
                return no_arg.newInstance();
            }
            catch (NoSuchMethodException e) {
                try {
                    Constructor<? extends StatementLocator> class_arg = value.getConstructor(Class.class);
                    return class_arg.newInstance(sqlObjectType);
                }
                catch (NoSuchMethodException e1) {
                    if (m != null) {
                        Constructor<? extends StatementLocator> c_m_arg = value.getConstructor(Class.class,
                                                                                               Method.class);
                        return c_m_arg.newInstance(sqlObjectType, m);
                    }
                    throw new IllegalStateException("Unable to instantiate, no viable constructor " + value.getName());
                }
            }

        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, Object arg)
        {
            throw new UnsupportedOperationException("Not applicable to parameter");
        }
    }
}

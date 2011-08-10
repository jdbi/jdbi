package org.skife.jdbi.v2.sqlobject.customizers;

import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.tweak.ArgumentFactory;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Used to set attributes on the StatementContext for the statement generated for this method.
 * These values will be available to other customizers, such as the statement locator or rewriter.
 */
@SqlStatementCustomizingAnnotation(RegisterArgumentFactory.Factory.class)
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterArgumentFactory
{
    /**
     * The key for the attribute to set. The value will be the value passed to the annotated argument
     */
    Class<? extends ArgumentFactory>[] value();

    static class Factory implements SqlStatementCustomizerFactory
    {
        public SqlStatementCustomizer createForType(Annotation annotation, Class sqlObjectType)
        {
            return create(annotation);
        }

        public SqlStatementCustomizer createForMethod(Annotation annotation, Class sqlObjectType, Method method)
        {
            return create(annotation);
        }

        public SqlStatementCustomizer createForParameter(Annotation annotation, Class sqlObjectType, Method method, final Object arg)
        {
            throw new IllegalStateException("not allowed on parameter");
        }

        private SqlStatementCustomizer create(Annotation annotation)
        {
            final RegisterArgumentFactory raf = (RegisterArgumentFactory) annotation;
            final List<ArgumentFactory> ary = new ArrayList<ArgumentFactory>(raf.value().length);
            for (Class<? extends ArgumentFactory> aClass : raf.value()) {
                try {
                    ary.add(aClass.newInstance());
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to instantiate specified argument factory", e);
                }
            }
            return new SqlStatementCustomizer()
            {
                public void apply(SQLStatement q) throws SQLException
                {
                    for (ArgumentFactory argumentFactory : ary) {
                        q.registerArgumentFactory(argumentFactory);
                    }
                }
            };

        }
    }
}

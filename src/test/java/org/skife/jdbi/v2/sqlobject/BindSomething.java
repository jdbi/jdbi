package org.skife.jdbi.v2.sqlobject;


import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.binders.Binder;
import org.skife.jdbi.v2.sqlobject.binders.BinderFactory;
import org.skife.jdbi.v2.sqlobject.binders.BindingAnnotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindSomething.Factory.class)
public @interface BindSomething
{
    String value();

    static class Factory implements BinderFactory
    {
        public Binder build(Annotation annotation)
        {
            return new Binder()
            {
                public void bind(SQLStatement q, Annotation bind, Object arg)
                {
                    BindSomething bs = (BindSomething) bind;
                    Something it = (Something) arg;
                    q.bind(bs.value() + ".id", it.getId());
                    q.bind(bs.value() + ".name", it.getName());
                }
            };
        }
    }

}

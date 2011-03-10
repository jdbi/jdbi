package org.skife.jdbi.v2.sqlobject.binders;

import org.skife.jdbi.v2.SQLStatement;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
@BindingAnnotation(BindBean.Factory.class)
public @interface BindBean
{
    String value() default "___jdbi_bare___";

    static class Factory implements BinderFactory
    {
        public Binder build(Annotation annotation)
        {
            return new Binder<BindBean, Object>()
            {
                public void bind(SQLStatement q, BindBean bind, Object arg)
                {
                    final String prefix;
                    if ("___jdbi_bare___".equals(bind.value())) {
                        prefix = "";
                    }
                    else {
                        prefix = bind.value() + ".";
                    }

                    try {
                        BeanInfo infos = Introspector.getBeanInfo(arg.getClass());
                        PropertyDescriptor[] props = infos.getPropertyDescriptors();
                        for (PropertyDescriptor prop : props) {
                            q.bind(prefix + prop.getName(), prop.getReadMethod().invoke(arg));
                        }
                    }
                    catch (Exception e) {
                        throw new IllegalStateException("unable to bind bean properties", e);
                    }


                }
            };
        }
    }

}

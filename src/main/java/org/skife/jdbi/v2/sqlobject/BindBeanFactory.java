package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

class BindBeanFactory implements BinderFactory
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
                        Method readMethod = prop.getReadMethod();
                        if (readMethod != null) {
                            q.bind(prefix + prop.getName(), readMethod.invoke(arg));
                        }
                    }
                }
                catch (Exception e) {
                    throw new IllegalStateException("unable to bind bean properties", e);
                }


            }
        };
    }
}

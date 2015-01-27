/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.SQLStatement;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

class BindBeanFactory implements BinderFactory
{
    @Override
    public Binder build(Annotation annotation)
    {
        return new Binder<BindBean, Object>()
        {
            @Override
            public void bind(SQLStatement q, BindBean bind, Object arg)
            {
                bindBeanProperties(q, bind.value(), arg);
            }
        };
    }

    static void bindBeanProperties(SQLStatement q, String placeholder, Object arg)
    {
        final String prefix;
        if ("___jdbi_bare___".equals(placeholder)) {
            prefix = "";
        }
        else {
            prefix = placeholder + ".";
        }

        try {
            BeanInfo infos = Introspector.getBeanInfo(arg.getClass());
            PropertyDescriptor[] props = infos.getPropertyDescriptors();
            for (PropertyDescriptor prop : props) {
                Method readMethod = prop.getReadMethod();
                if (readMethod != null) {
                    q.dynamicBind(readMethod.getReturnType(), prefix + prop.getName(), readMethod.invoke(arg));
                }
            }
        }
        catch (Exception e) {
            throw new IllegalStateException("unable to bind bean properties", e);
        }
    }
}

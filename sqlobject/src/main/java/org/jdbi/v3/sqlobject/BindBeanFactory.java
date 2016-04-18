/*
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
package org.jdbi.v3.sqlobject;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Objects;

class BindBeanFactory implements BinderFactory<BindBean, Object>
{
    @Override
    public Binder<BindBean, Object> build(BindBean annotation)
    {
        return (q, param, index, bind, arg) -> {
            final String prefix;
            if (BindBean.BARE_BINDING.equals(bind.value())) {
                prefix = "";
            }
            else {
                prefix = bind.value() + ".";
            }

            try {
                BeanInfo infos = Introspector.getBeanInfo(arg.getClass());
                PropertyDescriptor[] props = infos.getPropertyDescriptors();
                for (PropertyDescriptor prop : props) {
                    if (Objects.equals("class", prop.getName())) {
                        continue;
                    }
                    Method readMethod = prop.getReadMethod();
                    if (readMethod != null) {
                        q.bindByType(
                                prefix + prop.getName(),
                                readMethod.invoke(arg),
                                readMethod.getGenericReturnType());
                    }
                }
            }
            catch (Exception e) {
                throw new IllegalStateException("unable to bind bean properties", e);
            }
        };
    }
}

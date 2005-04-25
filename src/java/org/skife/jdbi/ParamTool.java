/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Collection;
import java.util.Iterator;

final class ParamTool
{
    final static Object[] EMPTY_OBJECT_ARRAY = new Object[] {};

    final static Object[] getParamsFromCollection(final Collection c)
    {
        final Object[] objects = new Object[c.size()];
        int count = 0;
        for (final Iterator iterator = c.iterator(); iterator.hasNext();)
        {
            final Object o = iterator.next();
            objects[count++] = o;
        }
        return objects;
    }

    final static Object[] getParamsFromMap(final String[] param_names, final Map map, final Map globals)
    {
        final Object[] objects = new Object[param_names.length];
        for (int i = 0; i < param_names.length; i++)
        {
            final String name = param_names[i];
            objects[i] = map.get(name);
            if (objects[i] == null)
            {
                objects[i] = globals.get(name);
            }
        }
        return objects;
    }

    final static Object[] getParamsForBean(final String[] param_names, final Object bean, final Map globals)
    {
        try
        {
            final BeanInfo info = Introspector.getBeanInfo(bean.getClass(), Object.class);
            final PropertyDescriptor[] pds = info.getPropertyDescriptors();
            final Method[] getters = new Method[param_names.length];
            for (int i = 0; i < param_names.length; i++)
            {
                final String param_name = param_names[i];
                for (int j = 0; j < pds.length; j++)
                {
                    final PropertyDescriptor pd = pds[j];
                    if (pd.getName().equals(param_name))
                    {
                        getters[i] = pd.getReadMethod();
                        break;
                    }
                }
            }

            final Object[] params = new Object[getters.length];
            for (int i = 0; i < getters.length; i++)
            {
                if (getters[i] != null)
                {
                    params[i] = getters[i].invoke(bean, EMPTY_OBJECT_ARRAY);
                }
                else
                {
                    params[i] = globals.get(param_names[i]);
                }
            }
            return params;
        }
        catch (IntrospectionException e)
        {
            throw new DBIError("exception setting parameters: " + e.getMessage(), e);
        }
        catch (IllegalAccessException e)
        {
            throw new DBIError("exception setting parameters: " + e.getMessage(), e);
        }
        catch (InvocationTargetException e)
        {
            throw new DBIError("exception setting parameters: " + e.getMessage(), e);
        }
    }
}

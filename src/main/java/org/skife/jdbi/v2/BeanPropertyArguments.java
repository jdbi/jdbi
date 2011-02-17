/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.tweak.Argument;
import org.skife.jdbi.v2.tweak.NamedArgumentFinder;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 */
class BeanPropertyArguments implements NamedArgumentFinder
{
    private final Object bean;
    private final StatementContext ctx;
    private BeanInfo info;

    BeanPropertyArguments(Object bean, StatementContext ctx)
    {
        this.bean = bean;
        this.ctx = ctx;
        try
        {
            this.info = Introspector.getBeanInfo(bean.getClass());
        }
        catch (IntrospectionException e)
        {
            throw new UnableToCreateStatementException("Failed to introspect object which is supposed ot be used to" +
                                                       " set named args for a statement via JavaBean properties", e, ctx);
        }

    }

    public Argument find(String name)
    {
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors())
        {
            if (descriptor.getName().equals(name))
            {
                try
                {
                    return new ObjectArgument(descriptor.getReadMethod().invoke(bean));
                }
                catch (IllegalAccessException e)
                {
                    throw new UnableToCreateStatementException(String.format("Access excpetion invoking getter for " +
                                                                             "bean property [%s] on [%s]",
                                                                             name, bean), e, ctx);
                }
                catch (InvocationTargetException e)
                {
                    throw new UnableToCreateStatementException(String.format("Invocation target exception invoking " +
                                                                             "getter for bean property [%s] on [%s]",
                                                                             name, bean), e, ctx);
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{lazy bean proprty arguments \"").append(bean).append("\"").toString();
    }
}

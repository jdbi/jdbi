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
package org.jdbi.v3;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import org.jdbi.v3.argument.Argument;
import org.jdbi.v3.argument.NamedArgumentFinder;
import org.jdbi.v3.exception.UnableToCreateStatementException;

class BeanPropertyArguments implements NamedArgumentFinder
{
    private final Object bean;
    private final StatementContext ctx;
    private final ArgumentRegistry argumentRegistry;
    private BeanInfo info;

    BeanPropertyArguments(Object bean, StatementContext ctx, ArgumentRegistry argumentRegistry)
    {
        this.bean = bean;
        this.ctx = ctx;
        this.argumentRegistry = argumentRegistry;
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

    @Override
    public Optional<Argument> find(String name)
    {
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors())
        {
            if (descriptor.getName().equals(name))
            {
                try
                {
                    return argumentRegistry.findArgumentFor(
                            descriptor.getReadMethod().getGenericReturnType(),
                            descriptor.getReadMethod().invoke(bean),
                            ctx);
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
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "{lazy bean proprty arguments \"" + bean + "\"";
    }
}

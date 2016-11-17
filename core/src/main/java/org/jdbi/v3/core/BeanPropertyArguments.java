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
package org.jdbi.v3.core;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.Arguments;
import org.jdbi.v3.core.argument.NamedArgumentFinder;
import org.jdbi.v3.core.exception.UnableToCreateStatementException;

class BeanPropertyArguments implements NamedArgumentFinder
{
    private final String prefix;
    private final Object bean;
    private final StatementContext ctx;
    private BeanInfo info;

    BeanPropertyArguments(String prefix, Object bean, StatementContext ctx)
    {
        this.prefix = (prefix == null || prefix.isEmpty()) ? "" : prefix + ".";
        this.bean = bean;
        this.ctx = ctx;
        try
        {
            this.info = Introspector.getBeanInfo(bean.getClass());
        }
        catch (IntrospectionException e)
        {
            throw new UnableToCreateStatementException("Failed to introspect object which is supposed to be used to " +
                                                       "set named args for a statement via JavaBean properties", e, ctx);
        }
    }

    @Override
    public Optional<Argument> find(String name)
    {
        if (name.startsWith(prefix)) {
            String propertyName = name.substring(prefix.length());
            for (PropertyDescriptor descriptor : info.getPropertyDescriptors())
            {
                if (propertyName.equals(descriptor.getName()))
                {
                    Method getter = descriptor.getReadMethod();
                    if (getter == null)
                    {
                        throw new UnableToCreateStatementException(String.format("No getter method found for " +
                                        "bean property [%s] on [%s]",
                                propertyName, bean), ctx);
                    }

                    try
                    {
                        return ctx.getConfig(Arguments.class)
                                .findFor(getter.getGenericReturnType(), getter.invoke(bean), ctx);
                    }
                    catch (IllegalAccessException e)
                    {
                        throw new UnableToCreateStatementException(String.format("Access exception invoking getter for " +
                                        "bean property [%s] on [%s]",
                                propertyName, bean), e, ctx);
                    }
                    catch (InvocationTargetException e)
                    {
                        throw new UnableToCreateStatementException(String.format("Invocation target exception invoking " +
                                        "getter for bean property [%s] on [%s]",
                                propertyName, bean), e, ctx);
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + bean + "\"";
    }
}

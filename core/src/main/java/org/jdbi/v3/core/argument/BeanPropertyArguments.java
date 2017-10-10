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
package org.jdbi.v3.core.argument;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Optional;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

/**
 * Inspect a {@link java.beans} style object and bind parameters
 * based on each of its discovered properties.
 */
public class BeanPropertyArguments extends FunctionReturnValueNamedArgumentFinder
{
    private BeanInfo info;

    /**
     * @param prefix an optional prefix (we insert a '.' as a separator)
     * @param bean the bean to inspect and bind
     */
    public BeanPropertyArguments(String prefix, Object bean)
    {
        super(prefix, bean);
        try
        {
            this.info = Introspector.getBeanInfo(bean.getClass());
        }
        catch (IntrospectionException e)
        {
            throw new UnableToCreateStatementException("Failed to introspect object which is supposed to be used to " +
                                                       "set named args for a statement via JavaBean properties", e);
        }
    }

    @Override
    public Optional<Argument> find0(String name, StatementContext ctx)
    {
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors())
        {
            if (name.equals(descriptor.getName()))
            {
                Method getter = descriptor.getReadMethod();
                if (getter == null)
                {
                    throw new UnableToCreateStatementException(String.format("No getter method found for " +
                                    "bean property [%s] on [%s]",
                            name, object), ctx);
                }

                return getArgumentForMethod(getter, ctx);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "{lazy bean property arguments \"" + object + "\"";
    }
}

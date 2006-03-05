package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.tweak.Argument;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

/**
 * 
 */
class BeanPropertyArguments implements LazyArguments
{
    private final Object bean;
    private BeanInfo info;

    BeanPropertyArguments(Object bean)
    {
        this.bean = bean;
        try
        {
            this.info = Introspector.getBeanInfo(bean.getClass());
        }
        catch (IntrospectionException e)
        {
            throw new UnableToCreateStatementException("Failed to introspect object which is supposed ot be used to" +
                                                       " set named args for a statement via JavaBean properties", e);
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
                                                                             name, bean), e);
                }
                catch (InvocationTargetException e)
                {
                    throw new UnableToCreateStatementException(String.format("Invocation target exception invoking " +
                                                                             "getter for bean property [%s] on [%s]",
                                                                             name, bean), e);
                }
            }
        }
        return null;
    }
}

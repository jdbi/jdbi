package org.skife.jdbi.v2;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * A result set mapper which maps the fields in a statement into a JavaBean. This uses
 * the JDK's built in bean mapping facilities, so it does not support nested properties.
 */
public class BeanMapper<T> extends BaseResultSetMapper<T>
{
    private BeanInfo info;
    private Class<T> type;

    public BeanMapper(Class<T> type)
    {
        this.type = type;
        try
        {
            info = Introspector.getBeanInfo(type);
        }
        catch (IntrospectionException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    protected T mapInternal(int index, Map<String, Object> row)
    {
        T bean;
        try
        {
            bean = type.newInstance();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(String.format("A bean, %s, was mapped " +
                                                             "which was not instantiable", type.getName()),
                                               e);
        }
        for (PropertyDescriptor descriptor : info.getPropertyDescriptors())
        {
            String name = descriptor.getName();
            if (row.containsKey(name))
            {
                try
                {
                    descriptor.getWriteMethod().invoke(bean, row.get(name));
                }
                catch (IllegalAccessException e)
                {
                    throw new IllegalArgumentException(String.format("Unable to access setter for " +
                                                                     "property, %s", name), e);
                }
                catch (InvocationTargetException e)
                {
                    throw new IllegalArgumentException(String.format("Invocation target exception trying to " +
                                                                     "invoker setter for the %s property", name), e);
                }
            }
        }
        return bean;
    }
}

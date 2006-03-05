package org.skife.jdbi.v2;

import junit.framework.TestCase;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 
 */
public class TestAnnotationMagic extends TestCase
{
    public void testSomething() throws Exception
    {
        Method m = Wombat.class.getMethod("m");
        Type return_type = m.getGenericReturnType();
        ParameterizedType parameterized_return_type = (ParameterizedType) return_type;
        Type parameterized_type_on_return_type = parameterized_return_type.getActualTypeArguments()[0];
        Class we_require_this_be_instantiable = (Class) parameterized_type_on_return_type;
        Object it = we_require_this_be_instantiable.newInstance();
        assertTrue(it instanceof String);
    }

    public static class Wombat
    {
        public List<String> m()
        {
            return null;
        }
    }
}


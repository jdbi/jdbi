/*
 * Copyright (C) 2004 - 2013 Brian McCallister
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
package org.jdbi.v3.sqlobject;

import static org.junit.Assert.assertEquals;

import org.jdbi.v3.Binding;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.MemoryDatabase;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Update;
import org.junit.Rule;
import org.junit.Test;

public class TestBindBeanFactory
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    void dummyBindBean(@BindBean int wat) { }

    @Test
    public void testBindBeanFactory()
        throws Exception
    {
        BindBean bindBeanImpl = getClass().getDeclaredMethod("dummyBindBean", int.class)
                .getParameters()[0].getAnnotation(BindBean.class);

        BindBeanFactory factory = new BindBeanFactory();
        Binder<BindBean, Object> beanBinder = factory.build(bindBeanImpl);

        final DBI dbi = db.getDbi();
        final Handle handle = dbi.open();
        final Update testStatement = handle.createStatement("does not matter");

        TestBean testBean = new TestBean();

        beanBinder.bind(testStatement, null, bindBeanImpl, testBean);

        StatementContext context = testStatement.getContext();
        Binding binding = context.getBinding();

        assertEquals("LongArgument", binding.forName("ALong").getClass().getSimpleName());
        assertEquals("BooleanArgument", binding.forName("ARealBoolean").getClass().getSimpleName());
        assertEquals("BooleanArgument", binding.forName("ANullBoolean").getClass().getSimpleName());
        assertEquals("StringArgument", binding.forName("AString").getClass().getSimpleName());
        assertEquals("ObjectArgument", binding.forName("AFoo").getClass().getSimpleName());
        assertEquals("ShortArgument", binding.forName("AShort").getClass().getSimpleName());

        handle.close();
    }

    public static class TestBean
    {
        public long getALong()
        {
            return 4815162342L;
        }

        public Boolean getARealBoolean()
        {
            return Boolean.TRUE;
        }

        public Boolean getANullBoolean()
        {
            return null;
        }

        public String getAString()
        {
            return "a string, what did you expect?";
        }

        public Foo getAFoo()
        {
            return new Foo();
        }

        public Short getAShort()
        {
            return Short.valueOf((short) 12345);
        }
    }

    public static class Foo
    {
    }
}

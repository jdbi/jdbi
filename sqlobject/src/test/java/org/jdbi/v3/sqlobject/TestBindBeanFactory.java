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

import static org.mockito.Mockito.verify;

import java.sql.PreparedStatement;
import java.sql.Types;

import org.jdbi.v3.Binding;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.ObjectArgumentFactory;
import org.jdbi.v3.StatementContext;
import org.jdbi.v3.Update;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestBindBeanFactory
{
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Mock
    PreparedStatement stmt;

    void dummyBindBean(@BindBean int wat) { }

    @Test
    public void testBindBeanFactory()
        throws Exception
    {
        TestBean testBean = new TestBean();

        BindBean bindBeanImpl = getClass().getDeclaredMethod("dummyBindBean", int.class)
                .getParameters()[0].getAnnotation(BindBean.class);

        BindBeanFactory factory = new BindBeanFactory();
        Binder<BindBean, Object> beanBinder = factory.build(bindBeanImpl);

        try (final Handle handle = db.openHandle()) {
            handle.registerArgumentFactory(ObjectArgumentFactory.create(Foo.class));
            final Update testStatement = handle.createStatement("does not matter");

            beanBinder.bind(testStatement, null, 0, bindBeanImpl, testBean);

            StatementContext context = testStatement.getContext();
            Binding binding = context.getBinding();

            binding.findForName("ALong").get().apply(1, stmt, null);
            binding.findForName("ARealBoolean").get().apply(2, stmt, null);
            binding.findForName("ANullBoolean").get().apply(3, stmt, null);
            binding.findForName("AString").get().apply(4, stmt, null);
            binding.findForName("AFoo").get().apply(5, stmt, null);
            binding.findForName("AShort").get().apply(6, stmt, null);
        }

        verify(stmt).setLong(1, testBean.getALong());
        verify(stmt).setBoolean(2, testBean.getARealBoolean());
        verify(stmt).setNull(3, Types.BOOLEAN);
        verify(stmt).setString(4, testBean.getAString());
        verify(stmt).setObject(5, testBean.getAFoo());
        verify(stmt).setShort(6, testBean.getAShort());
    }

    public static class TestBean
    {
        private final Foo foo = new Foo();

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
            return foo;
        }

        public Short getAShort()
        {
            return (short) 12345;
        }
    }

    public static class Foo
    {
    }
}

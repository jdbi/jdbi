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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class TestBeanArguments {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    PreparedStatement stmt;

    StatementContext ctx = StatementContextAccess.createContext();

    @Test
    public void testBindBare() throws Exception {
        new BeanPropertyArguments("", new Foo(BigDecimal.ONE))
            .find("foo", ctx)
            .get()
            .apply(5, stmt, null);

        verify(stmt).setBigDecimal(5, BigDecimal.ONE);
    }

    @Test
    public void testBindNull() throws Exception {
        new BeanPropertyArguments("", new Foo(null))
            .find("foo", ctx)
            .get()
            .apply(3, stmt, null);

        verify(stmt).setNull(3, Types.NUMERIC);
    }

    public static class Foo {
        private final BigDecimal foo;

        Foo(BigDecimal foo) {
            this.foo = foo;
        }

        public BigDecimal getFoo() {
            return foo;
        }
    }

    @Test
    public void testBindPrefix() throws Exception {
        new BeanPropertyArguments("foo", new Bar())
            .find("foo.bar", ctx)
            .get()
            .apply(3, stmt, null);

        verify(stmt).setString(3, "baz");
    }

    public static class Bar {
        public String getBar() {
            return "baz";
        }
    }

    @Test
    public void testBindIllegalAccess() {
        assertThatThrownBy(() -> new BeanPropertyArguments("foo", new ThrowsIllegalAccessException()).find("foo.bar", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    public static class ThrowsIllegalAccessException {
        public String getBar() throws IllegalAccessException {
            throw new IllegalAccessException();
        }
    }

    @Test
    public void testBindNoGetter() {
        assertThatThrownBy(() -> new BeanPropertyArguments("foo", new NoGetter()).find("foo.bar", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    public static class NoGetter {
        @SuppressWarnings("unused")
        public void setBar(String bar) {}
    }

    @Test
    public void testBindNonPublicGetter() {
        assertThatThrownBy(() -> new BeanPropertyArguments("foo", new NonPublicGetter()).find("foo.bar", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    public static class NonPublicGetter {
        @SuppressWarnings("unused")
        protected String getBar() {
            return "baz";
        }

        @SuppressWarnings("unused")
        public void setBar(String bar) {}
    }

    @Test
    public void testBindNestedOptionalNull() throws Exception {
        new BeanPropertyArguments("", new FooProperty(null)).find("foo?.id", ctx).get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.OTHER);
    }

    @Test
    public void testBindNestedNestedOptionalNull() throws Exception {
        new BeanPropertyArguments("", new FooProperty(null)).find("foo?.bar.id", ctx).get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.OTHER);
    }

    @Test
    public void testBindNestedNestedNull() {
        assertThatThrownBy(() -> new BeanPropertyArguments("", new FooProperty(null))
                .find("foo.bar.id", ctx)
                .get()
                .apply(3, stmt, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBindNestedNestedWrongOptionalNull1() {
        assertThatThrownBy(() -> new BeanPropertyArguments("", new FooProperty(null))
            .find("foo.bar?.id", ctx)
            .get()
            .apply(3, stmt, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBindNestedNestedWrongOptionalNull2() {

        assertThatThrownBy(() -> new BeanPropertyArguments("", new FooProperty(null))
            .find("foo.bar.?id", ctx)
            .get()
            .apply(3, stmt, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBindNestedOptionalNonNull() throws Exception {
        Object bean = new FooProperty(new IdProperty(69));

        new BeanPropertyArguments("", bean).find("foo?.id", ctx).get().apply(3, stmt, null);

        verify(stmt).setLong(3, 69);
    }

    public static class FooProperty {
        private final Object foo;

        FooProperty(Object foo) {
            this.foo = foo;
        }

        @SuppressWarnings("unused")
        public Object getFoo() {
            return foo;
        }
    }

    public static class IdProperty {
        private final long id;

        IdProperty(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }
    }

    @Test
    public void testPrivateClass() throws Exception {
        new ObjectMethodArguments(null, Person.create("hello")).find("name", ctx).get().apply(4, stmt, null);
        verify(stmt).setString(4, "hello");
    }

    @Test
    public void testPrivateInterfaceClass() throws Exception {
        new ObjectMethodArguments(null, Car.create("hello")).find("name", ctx).get().apply(4, stmt, null);
        verify(stmt).setString(4, "hello");
    }

    public abstract static class Person {
        public static Person create(String name) {
            return new PersonImpl(name);
        }

        public abstract String name();

        private static class PersonImpl extends Person {
            private String name;

            PersonImpl(String name) {
                this.name = name;
            }

            @Override
            public String name() {
                return name;
            }
        }
    }

    public interface Car {
        static Car create(String name) {
            return new CarImpl(name);
        }

        String name();
    }

    private static class CarImpl implements Car {
        private String name;

        CarImpl(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }
}

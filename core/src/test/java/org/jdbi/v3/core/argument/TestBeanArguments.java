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

import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextAccess;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

public class TestBeanArguments {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    PreparedStatement stmt;

    StatementContext ctx = StatementContextAccess.createContext();

    @Test
    public void testBindBare() throws Exception {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public BigDecimal getFoo() {
                return BigDecimal.ONE;
            }
        };

        new BeanPropertyArguments("", bean).find("foo", ctx).get().apply(5, stmt, null);

        verify(stmt).setBigDecimal(5, BigDecimal.ONE);
    }

    @Test
    public void testBindNull() throws Exception {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public BigDecimal getFoo() {
                return null;
            }
        };

        new BeanPropertyArguments("", bean).find("foo", ctx).get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.NUMERIC);
    }

    @Test
    public void testBindPrefix() throws Exception {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public String getBar() {
                return "baz";
            }
        };

        new BeanPropertyArguments("foo", bean).find("foo.bar", ctx).get().apply(3, stmt, null);

        verify(stmt).setString(3, "baz");
    }

    @Test
    public void testBindIllegalAccess() {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public String getBar() throws IllegalAccessException {
                throw new IllegalAccessException();
            }
        };

        assertThatThrownBy(() -> new BeanPropertyArguments("foo", bean).find("foo.bar", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testBindNoGetter() {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public void setBar(String bar) {}
        };

        assertThatThrownBy(() -> new BeanPropertyArguments("foo", bean).find("foo.bar", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testBindNonPublicGetter() {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            protected String getBar() {
                return "baz";
            }

            @SuppressWarnings("unused")
            public void setBar(String bar) {}
        };

        assertThatThrownBy(() -> new BeanPropertyArguments("foo", bean).find("foo.bar", ctx))
            .isInstanceOf(UnableToCreateStatementException.class);
    }

    @Test
    public void testBindNestedOptionalNull() throws Exception {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public Object getFoo() {
                return null;
            }
        };

        new BeanPropertyArguments("", bean).find("foo?.id", ctx).get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.OTHER);
    }

    @Test
    public void testBindNestedNestedOptionalNull() throws Exception {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public Object getFoo() {
                return null;
            }
        };

        new BeanPropertyArguments("", bean).find("foo?.bar.id", ctx).get().apply(3, stmt, null);

        verify(stmt).setNull(3, Types.OTHER);
    }

    @Test
    public void testBindNestedNestedNull() {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public Object getFoo() {
                return null;
            }
        };

        assertThatThrownBy(() -> new BeanPropertyArguments("", bean).find("foo.bar.id", ctx).get().apply(3, stmt, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBindNestedNestedWrongOptionalNull1() {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public Object getFoo() {
                return null;
            }
        };

        assertThatThrownBy(() -> new BeanPropertyArguments("", bean).find("foo.bar?.id", ctx).get().apply(3, stmt, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBindNestedNestedWrongOptionalNull2() {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public Object getFoo() {
                return null;
            }
        };

        assertThatThrownBy(() -> new BeanPropertyArguments("", bean).find("foo.bar.?id", ctx).get().apply(3, stmt, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testBindNestedOptionalNonNull() throws Exception {
        Object bean = new Object() {
            @SuppressWarnings("unused")
            public Object getFoo() {
                return new Object() {
                    public long getId() {
                        return 69;
                    }
                };
            }
        };

        new BeanPropertyArguments("", bean).find("foo?.id", ctx).get().apply(3, stmt, null);

        verify(stmt).setLong(3, 69);
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

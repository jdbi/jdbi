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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestSqlMethodDecoratingAnnotations {
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Handle handle;

    private static final ThreadLocal<List<String>> invocations = ThreadLocal.withInitial(ArrayList::new);

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
        invocations.get().clear();
    }

    @Test
    public void testUnordered() throws Exception {
        Dao dao = handle.attach(Dao.class);
        dao.unordered();

        assertThat(invocations.get()).isIn(asList("foo", "bar", "method"), asList("bar", "foo", "method"));
    }

    @Test
    public void testOrderedFooBar() throws Exception {
        Dao dao = handle.attach(Dao.class);
        dao.orderedFooBar();

        assertThat(invocations.get()).containsExactly("foo", "bar", "method");
    }

    @Test
    public void testOrderedBarFoo() throws Exception {
        Dao dao = handle.attach(Dao.class);
        dao.orderedBarFoo();

        assertThat(invocations.get()).containsExactly("bar", "foo", "method");
    }

    @Test
    public void testOrderedFooBarOnType() {
        OrderedOnType dao = handle.attach(OrderedOnType.class);
        dao.orderedFooBarOnType();

        assertThat(invocations.get()).containsExactly("foo", "bar", "method");
    }

    @Test
    public void testOrderedFooBarOnTypeOverriddenToBarFooOnMethod() {
        OrderedOnType dao = handle.attach(OrderedOnType.class);
        dao.orderedBarFooOnMethod();

        assertThat(invocations.get()).containsExactly("bar", "foo", "method");
    }

    @Test
    public void testAbortingDecorator() {
        Dao dao = handle.attach(Dao.class);
        dao.abortingDecorator();

        assertThat(invocations.get()).containsExactly("foo", "abort");
    }

    static void invoked(String value) {
        invocations.get().add(value);
    }

    public interface Dao {
        @Foo
        @Bar
        @CustomSqlMethod
        void unordered();

        @Foo
        @Bar
        @CustomSqlMethod
        @DecoratorOrder({Foo.class, Bar.class})
        void orderedFooBar();

        @Foo
        @Bar
        @CustomSqlMethod
        @DecoratorOrder({Bar.class, Foo.class})
        void orderedBarFoo();

        @Foo
        @Abort
        @Bar
        @CustomSqlMethod
        @DecoratorOrder({Foo.class, Abort.class, Bar.class})
        void abortingDecorator();
    }

    @DecoratorOrder({Foo.class, Bar.class})
    public interface OrderedOnType {
        @Foo
        @Bar
        @CustomSqlMethod
        void orderedFooBarOnType();

        @Foo
        @Bar
        @CustomSqlMethod
        @DecoratorOrder({Bar.class, Foo.class})
        void orderedBarFooOnMethod();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlMethodDecoratingAnnotation(Foo.Factory.class)
    public @interface Foo {
        class Factory implements HandlerDecorator {
            @Override
            public Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method) {
                return (obj, m, args, config, handle) -> {
                    invoked("foo");
                    return base.invoke(obj, m, args, config, handle);
                };
            }
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlMethodDecoratingAnnotation(Bar.Factory.class)
    public @interface Bar {
        class Factory implements HandlerDecorator {
            @Override
            public Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method) {
                return (obj, m, args, config, handle) -> {
                    invoked("bar");
                    return base.invoke(obj, m, args, config, handle);
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlMethodDecoratingAnnotation(Abort.Factory.class)
    public @interface Abort {
        class Factory implements HandlerDecorator {
            @Override
            public Handler decorateHandler(Handler base, Class<?> sqlObjectType, Method method) {
                return (obj, m, args, config, handle) -> {
                    invoked("abort");
                    return null;
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @SqlMethodAnnotation(CustomSqlMethod.Factory.class)
    public @interface CustomSqlMethod {
        class Factory implements HandlerFactory {
            @Override
            public Handler buildHandler(Class<?> sqlObjectType, Method method) {
                return (obj, m, args, config, handle) -> {
                    invoked("method");
                    return null;
                };
            }
        }
    }
}

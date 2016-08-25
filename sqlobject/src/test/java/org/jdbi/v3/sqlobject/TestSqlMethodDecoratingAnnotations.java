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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
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

    private Dao dao;

    @Before
    public void setUp() throws Exception {
        handle = db.getSharedHandle();
        invocations.get().clear();
        dao = handle.attach(Dao.class);
    }

    @Test
    public void testUnordered() throws Exception {
        dao.unordered();

        List<String> invocations = TestSqlMethodDecoratingAnnotations.invocations.get();
        assertTrue(invocations.contains("foo"));
        assertTrue(invocations.indexOf("foo") < 2);

        assertTrue(invocations.contains("bar"));
        assertTrue(invocations.indexOf("bar") < 2);

        assertTrue(invocations.indexOf("method") == 2);
    }

    @Test
    public void testOrderedFooBar() throws Exception {
        dao.orderedFooBar();

        List<String> invocations = TestSqlMethodDecoratingAnnotations.invocations.get();
        assertThat(invocations, hasItems("foo", "bar", "method"));
    }

    @Test
    public void testOrderedBarFoo() throws Exception {
        dao.orderedBarFoo();

        List<String> invocations = TestSqlMethodDecoratingAnnotations.invocations.get();
        assertThat(invocations, hasItems("bar", "foo", "method"));
    }

    static void invoked(String value) {
        invocations.get().add(value);
    }

    public interface Dao extends GetHandle {
        @Foo
        @Bar
        default void unordered() {
            invoked("method");
        }

        @Foo
        @Bar
        @DecoratorOrder({Foo.class, Bar.class})
        default void orderedFooBar() {
            invoked("method");
        }

        @Foo
        @Bar
        @DecoratorOrder({Bar.class, Foo.class})
        default void orderedBarFoo() {
            invoked("method");
        }
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
}

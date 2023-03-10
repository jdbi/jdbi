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
package org.jdbi.v3.core.extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.annotation.ExtensionCustomizationOrder;
import org.jdbi.v3.core.extension.annotation.UseExtensionCustomizer;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExtensionCustomizers {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle testHandle;

    private static final ThreadLocal<List<String>> INVOCATIONS = ThreadLocal.withInitial(ArrayList::new);

    @BeforeEach
    public void setUp() {
        testHandle = h2Extension.getSharedHandle();
        testHandle.getConfig(Extensions.class).register(new ExtensionFrameworkTestFactory());

        INVOCATIONS.get().clear();
    }

    @Test
    public void testUnordered() {
        Dao dao = testHandle.attach(Dao.class);
        dao.unordered();

        assertThat(INVOCATIONS.get()).isIn(asList("foo", "bar", "method"), asList("bar", "foo", "method"));
    }

    @Test
    public void testOrderedFooBar() {
        Dao dao = testHandle.attach(Dao.class);
        dao.orderedFooBar();

        assertThat(INVOCATIONS.get()).containsExactly("foo", "bar", "method");
    }

    @Test
    public void testOrderedBarFoo() {
        Dao dao = testHandle.attach(Dao.class);
        dao.orderedBarFoo();

        assertThat(INVOCATIONS.get()).containsExactly("bar", "foo", "method");
    }

    @Test
    public void testOrderedFooBarOnType() {
        OrderedOnType dao = testHandle.attach(OrderedOnType.class);
        dao.orderedFooBarOnType();

        assertThat(INVOCATIONS.get()).containsExactly("foo", "bar", "method");
    }

    @Test
    public void testOrderedFooBarOnTypeOverriddenToBarFooOnMethod() {
        OrderedOnType dao = testHandle.attach(OrderedOnType.class);
        dao.orderedBarFooOnMethod();

        assertThat(INVOCATIONS.get()).containsExactly("bar", "foo", "method");
    }

    @Test
    public void testTypeWrapsMethod() {
        TypeDecorator dao = testHandle.attach(TypeDecorator.class);
        dao.typeWrapsMethod();

        assertThat(INVOCATIONS.get()).containsExactly("foo", "bar", "method");
    }

    @Test
    public void testAbortingDecorator() {
        Dao dao = testHandle.attach(Dao.class);
        dao.abortingDecorator();

        assertThat(INVOCATIONS.get()).containsExactly("foo", "abort");
    }

    @Test
    public void testRegisteredDecorator() {
        testHandle.getConfig(Extensions.class).registerHandlerCustomizer(
                (base, sqlObjectType, method) ->
                        (handleSupplier, target, args) -> {
                            invoked("custom");
                            return base.invoke(handleSupplier, target, args);
                        });

        testHandle.attach(Dao.class).orderedFooBar();

        assertThat(INVOCATIONS.get()).containsExactlyInAnyOrder("custom", "foo", "bar", "method");
    }

    @Test
    public void testRegisteredDecoratorReturnsBase() {
        testHandle.getConfig(Extensions.class).registerHandlerCustomizer((base, sqlObjectType, method) -> base);

        testHandle.attach(Dao.class).orderedFooBar();

        assertThat(INVOCATIONS.get()).containsExactly("foo", "bar", "method");
    }

    static void invoked(String value) {
        INVOCATIONS.get().add(value);
    }

    public interface Dao {

        @Foo
        @Bar
        @CustomExtensionHandler
        void unordered();

        @Foo
        @Bar
        @CustomExtensionHandler
        @ExtensionCustomizationOrder({Foo.class, Bar.class})
        void orderedFooBar();

        @Foo
        @Bar
        @CustomExtensionHandler
        @ExtensionCustomizationOrder({Bar.class, Foo.class})
        void orderedBarFoo();

        @Foo
        @Abort
        @Bar
        @CustomExtensionHandler
        @ExtensionCustomizationOrder({Foo.class, Abort.class, Bar.class})
        void abortingDecorator();
    }

    @ExtensionCustomizationOrder({Foo.class, Bar.class})
    public interface OrderedOnType {

        @Foo
        @Bar
        @CustomExtensionHandler
        void orderedFooBarOnType();

        @Foo
        @Bar
        @CustomExtensionHandler
        @ExtensionCustomizationOrder({Bar.class, Foo.class})
        void orderedBarFooOnMethod();
    }

    @Foo
    public interface TypeDecorator {

        @Bar
        @CustomExtensionHandler
        void typeWrapsMethod();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionCustomizer(Foo.Factory.class)
    public @interface Foo {

        class Factory implements ExtensionHandlerCustomizer {

            @Override
            public ExtensionHandler customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
                return (handleSupplier, target, args) -> {
                    invoked("foo");
                    return base.invoke(handleSupplier, target, args);
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionCustomizer(Bar.Factory.class)
    public @interface Bar {

        class Factory implements ExtensionHandlerCustomizer {

            @Override
            public ExtensionHandler customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
                return (handleSupplier, target, args) -> {
                    invoked("bar");
                    return base.invoke(handleSupplier, target, args);
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionCustomizer(Abort.Factory.class)
    public @interface Abort {

        class Factory implements ExtensionHandlerCustomizer {

            @Override
            public ExtensionHandler customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
                return (handleSupplier, target, args) -> {
                    invoked("abort");
                    return null;
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandler(id = "test", value = CustomExtensionHandler.Impl.class)
    public @interface CustomExtensionHandler {

        class Impl implements ExtensionHandler {

            @Override
            public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) {
                invoked("method");
                return null;
            }
        }
    }
}

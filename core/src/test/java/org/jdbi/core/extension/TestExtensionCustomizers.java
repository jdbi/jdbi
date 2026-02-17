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
package org.jdbi.core.extension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.extension.annotation.ExtensionHandlerCustomizationOrder;
import org.jdbi.core.extension.annotation.UseExtensionHandler;
import org.jdbi.core.extension.annotation.UseExtensionHandlerCustomizer;
import org.jdbi.core.junit5.H2DatabaseExtension;
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
                        (config, target) -> (handleSupplier, args) -> {
                            invoked("custom");
                            return base.attachTo(config, target).invoke(handleSupplier, args);
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
        @ExtensionHandlerCustomizationOrder({Foo.class, Bar.class})
        void orderedFooBar();

        @Foo
        @Bar
        @CustomExtensionHandler
        @ExtensionHandlerCustomizationOrder({Bar.class, Foo.class})
        void orderedBarFoo();

        @Foo
        @Abort
        @Bar
        @CustomExtensionHandler
        @ExtensionHandlerCustomizationOrder({Foo.class, Abort.class, Bar.class})
        void abortingDecorator();
    }

    @ExtensionHandlerCustomizationOrder({Foo.class, Bar.class})
    public interface OrderedOnType {

        @Foo
        @Bar
        @CustomExtensionHandler
        void orderedFooBarOnType();

        @Foo
        @Bar
        @CustomExtensionHandler
        @ExtensionHandlerCustomizationOrder({Bar.class, Foo.class})
        void orderedBarFooOnMethod();
    }

    @Foo
    public interface TypeDecorator {

        @Bar
        @CustomExtensionHandler
        void typeWrapsMethod();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandlerCustomizer(Foo.Factory.class)
    public @interface Foo {

        class Factory implements ExtensionHandlerCustomizer {

            @Override
            public ExtensionHandler customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
                return (config, target) -> (handleSupplier, args) -> {
                    invoked("foo");
                    return base.attachTo(config, target).invoke(handleSupplier, args);
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandlerCustomizer(Bar.Factory.class)
    public @interface Bar {

        class Factory implements ExtensionHandlerCustomizer {

            @Override
            public ExtensionHandler customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
                return (config, target) -> (handleSupplier, args) -> {
                    invoked("bar");
                    return base.attachTo(config, target).invoke(handleSupplier, args);
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandlerCustomizer(Abort.Factory.class)
    public @interface Abort {

        class Factory implements ExtensionHandlerCustomizer {

            @Override
            public ExtensionHandler.Simple customize(ExtensionHandler base, Class<?> sqlObjectType, Method method) {
                return (handleSupplier, args) -> {
                    invoked("abort");
                    return null;
                };
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandler(id = "test", value = CustomExtensionHandler.Impl.class)
    public @interface CustomExtensionHandler {

        class Impl implements ExtensionHandler.Simple {
            @Override
            public Object invoke(HandleSupplier handleSupplier, Object... args) throws Exception {
                invoked("method");
                return null;
            }
        }
    }
}

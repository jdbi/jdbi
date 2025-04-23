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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.TestExtensionAnnotations.ExtensionOne.Impl;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestExtensionAnnotations {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.getConfig(Extensions.class).register(new ExtensionFrameworkTestFactory());
    }

    @Test
    public void testMutuallyExclusiveAnnotations() {
        assertThatThrownBy(() -> handle.attach(Broken.class)).isInstanceOf(IllegalStateException.class);
    }

    public interface Broken {

        @ExtensionOne
        @ExtensionTwo
        void bogus();
    }

    @Test
    public void testCustomAnnotation() {
        Dao dao = handle.attach(Dao.class);

        assertThat(dao.foo()).isEqualTo("foo");
    }

    public interface Dao {

        @Foo
        String foo();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandler(id = "test", value = Foo.Impl.class)
    public @interface Foo {

        class Impl implements ExtensionHandler {

            @Override
            public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) {
                return "foo";
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @UseExtensionHandler(id = "test", value = Impl.class)
    public @interface ExtensionOne {

        class Impl implements ExtensionHandler {

            @Override
            public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) throws Exception {
                return null;
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD})
    @UseExtensionHandler(id = "test", value = ExtensionTwo.Impl.class)
    public @interface ExtensionTwo {

        class Impl implements ExtensionHandler {

            @Override
            public Object invoke(HandleSupplier handleSupplier, Object target, Object... args) throws Exception {
                return null;
            }
        }
    }
}

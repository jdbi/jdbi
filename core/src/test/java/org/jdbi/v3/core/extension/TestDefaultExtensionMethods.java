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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestDefaultExtensionMethods {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
        handle.getConfig(Extensions.class).register(new ExtensionFrameworkTestFactory());
    }

    @Test
    public void testDefaultMethodToString() {
        DefaultMethodInterface dmo1 = handle.attach(DefaultMethodInterface.class);
        assertThat(dmo1.toString()).startsWith("Jdbi extension proxy for " + DefaultMethodInterface.class.getName());

        DefaultMethodInterface dmo2 = handle.attach(DefaultMethodInterface.class);

        assertThat(dmo1.toString()).startsWith("Jdbi extension proxy for " + DefaultMethodInterface.class.getName());
        assertThat(dmo2.toString()).startsWith("Jdbi extension proxy for " + DefaultMethodInterface.class.getName());
        assertThat(dmo1.equals(dmo2)).isFalse();
        assertThat(dmo2.equals(dmo1)).isFalse();

        assertThat(dmo1.hashCode()).isNotEqualTo(dmo2.hashCode());
        assertThat(dmo1.hashCode()).isEqualTo(System.identityHashCode(dmo1));
    }

    @Test
    public void testDefaultMethodEquals() {
        DefaultMethodInterface dmo1 = handle.attach(DefaultMethodInterface.class);
        DefaultMethodInterface dmo2 = handle.attach(DefaultMethodInterface.class);

        assertThat(dmo1.equals(dmo2)).isFalse();
        assertThat(dmo2.equals(dmo1)).isFalse();
    }

    @Test
    public void testDefaultMethodHashCode() {
        DefaultMethodInterface dmo1 = handle.attach(DefaultMethodInterface.class);
        DefaultMethodInterface dmo2 = handle.attach(DefaultMethodInterface.class);

        assertThat(dmo1.hashCode()).isNotEqualTo(dmo2.hashCode());
        assertThat(dmo1.hashCode()).isEqualTo(System.identityHashCode(dmo1));
        assertThat(dmo2.hashCode()).isEqualTo(System.identityHashCode(dmo2));
    }

    @Test
    public void testFinalizeOverride() throws Throwable {
        FinalizeInterface fm = handle.attach(FinalizeInterface.class);
        assertThat(fm.testFinalize()).isEqualTo("a ok");
    }

    public interface DefaultMethodInterface {
        @ForTest
        void testMethod();
    }

    public interface FinalizeInterface {
        @ForTest
        void testMethod();

        default String testFinalize() {
            this.finalize();
            return "a ok";
        }

        default void finalize() {}
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandler(id = "test", value = TestExtensionAnnotations.Foo.Impl.class)
    public @interface ForTest {
        class Impl implements ExtensionHandler.Simple {
            @Override
            public Object invoke(HandleSupplier handleSupplier, Object... args) {
                return "foo";
            }
        }
    }
}

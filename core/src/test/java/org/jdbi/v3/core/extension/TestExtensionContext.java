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

import java.lang.reflect.Method;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.junit5.H2DatabaseExtension.SOMETHING_INITIALIZER;

public class TestExtensionContext {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance()
            .withInitializer(SOMETHING_INITIALIZER)
            .withPlugin(new TestPlugin());

    static class TestPlugin implements JdbiPlugin {

        @Override
        public void customizeJdbi(Jdbi jdbi) {
            jdbi.registerExtension(new TestExtensionFactory());
        }
    }

    static class TestExtensionFactory implements ExtensionFactory {

        @Override
        public boolean accepts(Class<?> extensionType) {
            return TestExtension.class.equals(extensionType);
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
            return extensionType.cast(new TestExtensionImpl(handleSupplier));
        }
    }

    public interface TestExtension {

        Handle getHandle();

        default void checkContextInDefaultMethod() throws Exception {
            Method m = TestExtension.class.getMethod("checkContextInDefaultMethod");
            Handle handle = getHandle();
            assertThat(handle.getExtensionMethod()).isNotNull();
            assertThat(handle.getExtensionMethod().getMethod()).isEqualTo(m);
        }

        void checkContextInImplementedMethod() throws Exception;
    }

    public static class TestExtensionImpl implements TestExtension {

        private final HandleSupplier handleSupplier;

        TestExtensionImpl(HandleSupplier handleSupplier) {
            this.handleSupplier = handleSupplier;
        }

        @Override
        public Handle getHandle() {
            return handleSupplier.getHandle();
        }

        @Override
        public void checkContextInImplementedMethod() throws Exception {
            Method m = TestExtension.class.getMethod("checkContextInImplementedMethod");
            Handle handle = getHandle();
            assertThat(handle.getExtensionMethod()).isNotNull();
            assertThat(handle.getExtensionMethod().getMethod()).isEqualTo(m);
        }
    }

    private Jdbi jdbi;
    private TestExtension testExtension;

    @BeforeEach
    public void setUp() {
        jdbi = h2Extension.getJdbi();
        testExtension = jdbi.onDemand(TestExtension.class);
    }

    @Test
    public void testExtensionInDefaultMethod() throws Exception {
        testExtension.checkContextInDefaultMethod();
    }

    @Test
    public void testExtensionInImplementedMethod() throws Exception {
        testExtension.checkContextInImplementedMethod();
    }
}

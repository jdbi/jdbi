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
package jdbi.doc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionHandlerTest {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());

    Jdbi jdbi;

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();

        jdbi.configure(Extensions.class, e -> e.register(new TestExtensionFactory()));
    }

    @Test
    public void testSomething() {
        Something result = jdbi.withExtension(ExtensionType.class, e -> e.getSomething(5, "banana"));

        assertThat(result).isEqualTo(new Something(5, "banana"));
    }

    // tag::extension-type[]
    interface ExtensionType {
        Something getSomething(int id, String name);
    }
    // end::extension-type[]

    // tag::extension-factory[]
    public static class TestExtensionFactory implements ExtensionFactory {
        @Override
        public boolean accepts(Class<?> extensionType) {
            return extensionType == ExtensionType.class; // <1>
        }

        @Override
        public Collection<ExtensionHandlerFactory> getExtensionHandlerFactories(ConfigRegistry config) {
            return Collections.singleton(new TestExtensionHandlerFactory()); // <2>
        }
    }
    // end::extension-factory[]

    // tag::extension-handler-factory[]
    public static class TestExtensionHandlerFactory implements ExtensionHandlerFactory {
        @Override
        public boolean accepts(Class<?> extensionType, Method method) {
            return method.getName().equals("getSomething"); // <1>
        }

        @Override
        public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {
            return Optional.of(new TestExtensionHandler()); // <2>
        }
    }
    // end::extension-handler-factory[]

    // tag::extension-handler[]
    public static class TestExtensionHandler implements ExtensionHandler.Simple {
        @Override
        public Object invoke(HandleSupplier handleSupplier, Object... args) {
            return new Something((int) args[0], (String) args[1]); // <1>
        }
    }
    // end::extension-handler[]
}

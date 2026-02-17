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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.ExtensionHandler;
import org.jdbi.core.extension.ExtensionHandlerFactory;
import org.jdbi.core.extension.ExtensionMetadata;
import org.jdbi.core.extension.ExtensionMetadata.ExtensionHandlerInvoker;
import org.jdbi.core.extension.Extensions;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.core.extension.ExtensionFactory.FactoryFlag.DONT_USE_PROXY;

class ExtensionMetadataTest {

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
        Something result = jdbi.withExtension(ExtensionType.class, e -> e.getSomething(5, "elderberry"));
        assertThat(result).isEqualTo(new Something(5, "elderberry"));
    }

    //tag::extension[]
    @Test
    public void testIdOne() {
        Something idOne = jdbi.withExtension(ExtensionType.class, ExtensionType::getIdOne);
        assertThat(idOne).isEqualTo(new Something(1, "apple"));
    }

    interface ExtensionType {

        Something getSomething(int id, String name);

        Something getIdOne();  // <1>
    }

    public static class TestExtensionFactory implements ExtensionFactory {

        @Override
        public boolean accepts(Class<?> extensionType) {
            return extensionType == ExtensionType.class;
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {

            ExtensionMetadata extensionMetadata = handleSupplier.getConfig() // <2>
                    .get(Extensions.class).findMetadata(extensionType, this);

            return extensionType.cast(new ExtensionTypeImpl(extensionMetadata, handleSupplier)); // <3>
        }

        @Override
        public Set<FactoryFlag> getFactoryFlags() {
            return EnumSet.of(DONT_USE_PROXY); // <4>
        }

        @Override
        public Collection<ExtensionHandlerFactory> getExtensionHandlerFactories(ConfigRegistry config) {
            return Collections.singleton(new TestExtensionHandlerFactory()); // <5>
        }
    }

    public static class TestExtensionHandlerFactory implements ExtensionHandlerFactory {

        @Override
        public boolean accepts(Class<?> extensionType, Method method) {
            return method.getName().equals("getSomething");  // <6>
        }

        @Override
        public Optional<ExtensionHandler> createExtensionHandler(Class<?> extensionType, Method method) {
            return Optional.of((ExtensionHandler.Simple)
                    (handleSupplier, args) -> new Something((int) args[0], (String) args[1]));
        }
    }

    static class ExtensionTypeImpl implements ExtensionType {

        private final ExtensionHandlerInvoker getSomethingInvoker;

        private ExtensionTypeImpl(ExtensionMetadata extensionMetadata, HandleSupplier handleSupplier) {

            ConfigRegistry config = handleSupplier.getConfig();
            this.getSomethingInvoker = extensionMetadata.createExtensionHandlerInvoker(this,
                    JdbiClassUtils.methodLookup(ExtensionType.class, "getSomething", int.class, String.class), // <7>
                    handleSupplier, config);
        }

        @Override
        public Something getSomething(int id, String name) {
            return (Something) getSomethingInvoker.invoke(id, name);
        }

        @Override
        public Something getIdOne() {
            return (Something) getSomethingInvoker.invoke(1, "apple"); // <8>
        }
    }
    //end::extension[]
}

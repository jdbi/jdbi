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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.config.TestConfigRegistry.TestConfig;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the per-attach isolation contract of {@link ExtensionMetadata#createInstanceConfiguration(ConfigRegistry)}
 * and {@link ExtensionMetadata#createMethodConfiguration(Method, ConfigRegistry)}: every derivation returns a
 * private configuration, later derivations observe reconfiguration of the source registry, and mutation of a
 * derived configuration does not leak into the source or into other derivations.
 */
public class TestExtensionConfigIsolation {

    private ConfigRegistry root;
    private ExtensionMetadata metadata;
    private Method doThing;

    @BeforeEach
    public void setUp() throws Exception {
        root = new ConfigRegistry();
        Extensions extensions = root.get(Extensions.class);
        extensions.register(new ExtensionFrameworkTestFactory());
        metadata = extensions.findMetadata(Dao.class, new ExtensionFrameworkTestFactory());
        doThing = Dao.class.getMethod("doThing");
    }

    @Test
    public void everyDerivationIsPrivate() {
        ConfigRegistry instance1 = metadata.createInstanceConfiguration(root);
        ConfigRegistry instance2 = metadata.createInstanceConfiguration(root);
        assertThat(instance2).isNotSameAs(instance1);

        ConfigRegistry method1 = metadata.createMethodConfiguration(doThing, instance1);
        ConfigRegistry method2 = metadata.createMethodConfiguration(doThing, instance1);
        assertThat(method2).isNotSameAs(method1);
    }

    @Test
    public void laterDerivationObservesSourceReconfiguration() {
        metadata.createInstanceConfiguration(root).get(TestConfig.class);

        root.get(TestConfig.class).addList("late");

        ConfigRegistry derived = metadata.createInstanceConfiguration(root);
        assertThat(derived.get(TestConfig.class).getList()).containsExactly("late");
    }

    @Test
    public void mutationOfDerivedConfigDoesNotLeak() {
        ConfigRegistry instance1 = metadata.createInstanceConfiguration(root);
        instance1.get(TestConfig.class).addList("private");

        assertThat(root.get(TestConfig.class).getList()).isEmpty();
        ConfigRegistry instance2 = metadata.createInstanceConfiguration(root);
        assertThat(instance2.get(TestConfig.class).getList()).isEmpty();

        ConfigRegistry method1 = metadata.createMethodConfiguration(doThing, instance1);
        method1.get(TestConfig.class).addList("method-private");
        assertThat(instance1.get(TestConfig.class).getList()).containsExactly("private");
        ConfigRegistry method2 = metadata.createMethodConfiguration(doThing, instance1);
        assertThat(method2.get(TestConfig.class).getList()).containsExactly("private");
    }

    public interface Dao {
        @TestHandler
        void doThing();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @UseExtensionHandler(id = "test", value = NoOpHandler.class)
    public @interface TestHandler {}

    public static class NoOpHandler implements ExtensionHandler.Simple {
        @Override
        public Object invoke(HandleSupplier handleSupplier, Object... args) {
            return null;
        }
    }
}

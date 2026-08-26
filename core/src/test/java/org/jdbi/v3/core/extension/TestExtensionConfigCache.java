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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.annotation.UseExtensionHandler;
import org.jdbi.v3.core.extension.internal.ExtensionConfigCache;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExtensionConfigCache {

    // --- ExtensionConfigCache unit behavior -------------------------------------------------

    @Test
    public void memoizesInstanceConfigurationPerType() {
        ExtensionConfigCache cache = new ExtensionConfigCache();
        AtomicInteger calls = new AtomicInteger();
        Supplier<ConfigRegistry> factory = () -> {
            calls.incrementAndGet();
            return new ConfigRegistry();
        };

        ConfigRegistry first = cache.instanceConfiguration(String.class, factory);
        ConfigRegistry second = cache.instanceConfiguration(String.class, factory);

        assertThat(second).isSameAs(first);
        assertThat(calls).hasValue(1);

        ConfigRegistry other = cache.instanceConfiguration(Integer.class, factory);
        assertThat(other).isNotSameAs(first);
        assertThat(calls).hasValue(2);
    }

    @Test
    public void memoizesMethodConfigurationPerMethod() throws Exception {
        ExtensionConfigCache cache = new ExtensionConfigCache();
        AtomicInteger calls = new AtomicInteger();
        Supplier<ConfigRegistry> factory = () -> {
            calls.incrementAndGet();
            return new ConfigRegistry();
        };
        Method toString = Object.class.getMethod("toString");
        Method hashCode = Object.class.getMethod("hashCode");

        ConfigRegistry first = cache.methodConfiguration(toString, factory);
        assertThat(cache.methodConfiguration(toString, factory)).isSameAs(first);
        assertThat(calls).hasValue(1);

        assertThat(cache.methodConfiguration(hashCode, factory)).isNotSameAs(first);
        assertThat(calls).hasValue(2);
    }

    @Test
    public void createCopyStartsEmptyAndIndependent() {
        ExtensionConfigCache cache = new ExtensionConfigCache();
        ConfigRegistry cached = cache.instanceConfiguration(String.class, ConfigRegistry::new);

        ExtensionConfigCache copy = cache.createCopy();
        assertThat(copy).isNotSameAs(cache);

        AtomicInteger calls = new AtomicInteger();
        ConfigRegistry inCopy = copy.instanceConfiguration(String.class, () -> {
            calls.incrementAndGet();
            return new ConfigRegistry();
        });
        // the copy did not inherit the original entry: the factory runs again and yields a new instance
        assertThat(calls).hasValue(1);
        assertThat(inCopy).isNotSameAs(cached);
    }

    // --- wiring through ExtensionMetadata ---------------------------------------------------

    @Test
    public void derivedConfigurationsAreCachedPerSourceRegistry() throws Exception {
        ConfigRegistry root = new ConfigRegistry();
        Extensions extensions = root.get(Extensions.class);
        extensions.register(new ExtensionFrameworkTestFactory());
        ExtensionMetadata metadata = extensions.findMetadata(Dao.class, new ExtensionFrameworkTestFactory());

        // repeated attaches against the same registry (the on-demand case) reuse the derived configuration
        ConfigRegistry instance1 = metadata.createInstanceConfiguration(root);
        ConfigRegistry instance2 = metadata.createInstanceConfiguration(root);
        assertThat(instance2).isSameAs(instance1);

        // a different source registry (e.g. a per-Handle config in Handle#attach) derives its own copy
        ConfigRegistry fromCopy = metadata.createInstanceConfiguration(root.createCopy());
        assertThat(fromCopy).isNotSameAs(instance1);

        Method doThing = Dao.class.getMethod("doThing");
        ConfigRegistry method1 = metadata.createMethodConfiguration(doThing, instance1);
        ConfigRegistry method2 = metadata.createMethodConfiguration(doThing, instance1);
        assertThat(method2).isSameAs(method1);
        assertThat(metadata.createMethodConfiguration(doThing, instance1.createCopy())).isNotSameAs(method1);
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

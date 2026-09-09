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
package org.jdbi.v3.core.mapper;

import java.io.IOException;
import java.io.InputStream;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.mapper.immutables.JdbiImmutables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Immutables generated classes must be resolved through the class loader of the spec, not the class
 * loader of Jdbi itself. Plugin containers (Paper, OSGi, application servers) load application code in
 * a child loader that Jdbi's own loader can not see.
 */
@DisabledInNativeImage // a native image can not define classes at run time
public class ImmutablesIsolatedClassLoaderTest {

    private static final String ISOLATED_PACKAGE = "org.jdbi.v3.core.mapper.isolated.";
    private static final String SPEC_NAME = ISOLATED_PACKAGE + "IsolatedTrain";

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private final IsolatingClassLoader loader = new IsolatingClassLoader();

    private Handle handle;
    private Class<?> specType;

    @BeforeEach
    public void setUp() throws Exception {
        handle = h2Extension.getSharedHandle();
        handle.execute("create table train (name varchar, carriages int)");
        handle.execute("insert into train (name, carriages) values ('Zephyr', 8)");

        specType = loader.loadClass(SPEC_NAME);
        assertThat(specType.getClassLoader()).isSameAs(loader);
    }

    @Test
    public void registerImmutableResolvesGeneratedClassThroughSpecLoader() {
        handle.getConfig(JdbiImmutables.class).registerImmutable(specType);

        Object train = handle.createQuery("select * from train").mapTo(specType).one();

        assertIsolatedTrain(train, "Immutable");
        assertRoundTrip(train);
    }

    @Test
    public void registerModifiableResolvesGeneratedClassThroughSpecLoader() throws Exception {
        handle.getConfig(JdbiImmutables.class).registerModifiable(specType);
        Class<?> modifiableType = loader.loadClass(ISOLATED_PACKAGE + "ModifiableIsolatedTrain");

        Object train = handle.createQuery("select * from train").mapTo(modifiableType).one();

        assertIsolatedTrain(train, "Modifiable");
        assertRoundTrip(train);
    }

    private void assertIsolatedTrain(Object train, String prefix) {
        assertThat(train).isInstanceOf(specType);
        assertThat(train.getClass().getClassLoader()).isSameAs(loader);
        assertThat(train.getClass().getName()).isEqualTo(ISOLATED_PACKAGE + prefix + "IsolatedTrain");
        assertThat(train).extracting("name", "carriages").containsExactly("Zephyr", 8);
    }

    private void assertRoundTrip(Object train) {
        assertThat(handle.createUpdate("insert into train (name, carriages) values (:name, :carriages)")
            .bindPojo(train)
            .execute())
            .isOne();

        assertThat(handle.createQuery("select count(1) from train where name = 'Zephyr' and carriages = 8").mapTo(int.class).one())
            .isEqualTo(2);
    }

    /**
     * Defines every class in the isolated package itself from the test class path and delegates all
     * other classes, including Jdbi, to the parent. This mirrors a plugin loader: the parent (Jdbi's
     * loader) can not resolve the isolated classes by name, only the child can.
     */
    static final class IsolatingClassLoader extends ClassLoader {

        IsolatingClassLoader() {
            super(ImmutablesIsolatedClassLoaderTest.class.getClassLoader());
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith(ISOLATED_PACKAGE)) {
                return super.loadClass(name, resolve);
            }
            synchronized (getClassLoadingLock(name)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = defineIsolated(name);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
        }

        private Class<?> defineIsolated(String name) throws ClassNotFoundException {
            String resource = name.replace('.', '/') + ".class";
            try (InputStream in = getParent().getResourceAsStream(resource)) {
                if (in == null) {
                    throw new ClassNotFoundException(name);
                }
                byte[] bytes = in.readAllBytes();
                return defineClass(name, bytes, 0, bytes.length);
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}

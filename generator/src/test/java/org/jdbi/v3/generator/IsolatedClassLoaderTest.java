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
package org.jdbi.v3.generator;

import java.lang.reflect.Method;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.internal.IsolatingClassLoader;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Generated sql object classes must be resolved through the class loader of the extension type, not the
 * class loader of Jdbi itself. Plugin containers (Paper, OSGi, application servers) load application code
 * in a child loader that Jdbi's own loader can not see.
 */
public class IsolatedClassLoaderTest {

    private static final String ISOLATED_PACKAGE = "org.jdbi.v3.generator.isolated";
    private static final String DAO_NAME = ISOLATED_PACKAGE + ".IsolatedDao";

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
        .withPlugins(new H2DatabasePlugin(), new SqlObjectPlugin())
        .withInitializer(TestingInitializers.something())
        .withConfig(Extensions.class, c -> c.setAllowProxy(false));

    @Test
    public void attachResolvesGeneratedClassThroughExtensionTypeLoader() throws Exception {
        IsolatingClassLoader loader = new IsolatingClassLoader(ISOLATED_PACKAGE);
        Class<?> daoType = loader.loadClass(DAO_NAME);
        assertThat(daoType.getClassLoader()).isSameAs(loader);

        Handle handle = h2Extension.getSharedHandle();
        Object dao = handle.attach(daoType);

        assertThat(dao).isInstanceOf(daoType);
        assertThat(dao.getClass().getClassLoader()).isSameAs(loader);
        assertThat(dao.getClass().getName()).isEqualTo(DAO_NAME + "Impl");

        exercise(daoType, dao);
    }

    @Test
    public void onDemandResolvesGeneratedClassThroughExtensionTypeLoader() throws Exception {
        IsolatingClassLoader loader = new IsolatingClassLoader(ISOLATED_PACKAGE);
        Class<?> daoType = loader.loadClass(DAO_NAME);

        Object dao = h2Extension.getJdbi().onDemand(daoType);

        assertThat(dao).isInstanceOf(daoType);
        assertThat(dao.getClass().getClassLoader()).isSameAs(loader);
        assertThat(dao.getClass().getName()).isEqualTo(DAO_NAME + "Impl$OnDemand");

        exercise(daoType, dao);
    }

    @SuppressWarnings("unchecked")
    private static void exercise(Class<?> daoType, Object dao) throws Exception {
        Method insert = daoType.getMethod("insert", int.class, String.class);
        Method names = daoType.getMethod("names");

        insert.invoke(dao, 1, "Bella");
        insert.invoke(dao, 2, "Ellie");

        assertThat((List<Object>) names.invoke(dao)).containsExactly("Bella", "Ellie");
    }
}

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
package org.jdbi.generator;

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jdbi.core.HandleCallback;
import org.jdbi.core.Something;
import org.jdbi.core.extension.Extensions;
import org.jdbi.core.h2.H2DatabasePlugin;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.GenerateSqlObject;
import org.jdbi.sqlobject.GeneratedSqlObjectProvider;
import org.jdbi.sqlobject.SqlObject;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The generator registers a {@link GeneratedSqlObjectProvider} for every generated class. The provider gives
 * Jdbi the generated class and its methods without reflection, which a GraalVM native image needs.
 */
public class GeneratedProviderTest {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugins(new H2DatabasePlugin(), new SqlObjectPlugin())
            .withInitializer(TestingInitializers.something())
            .withConfig(Extensions.class, c -> c.setAllowProxy(false));

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().registerRowMapper(new SomethingMapper());
    }

    @Test
    public void providersRegisteredForAllGeneratedTypes() {
        // IsolatedDao is named instead of referenced, so that this test does not define it in the parent
        // class loader and break IsolatedClassLoaderTest
        assertThat(loadProviders().keySet()).extracting(Class::getName).containsExactlyInAnyOrder(
                ProviderDao.class.getName(),
                ArrayBindingTest.BazDao.class.getName(),
                NonpublicSubclassTest.AbstractClassDao.class.getName(),
                NonpublicSubclassTest.InterfaceDao.class.getName(),
                "org.jdbi.generator.isolated.IsolatedDao");
    }

    @Test
    public void providerReportsExtensionMethods() throws Exception {
        GeneratedSqlObjectProvider provider = loadProviders().get(ProviderDao.class);

        assertThat(provider.extensionMethods()).containsExactlyInAnyOrder(
                ProviderDao.class.getMethod("insert", int.class, String.class),
                ProviderDao.class.getMethod("list"),
                ProviderDao.class.getMethod("count"),
                SqlObject.class.getMethod("getHandle"),
                SqlObject.class.getMethod("withHandle", HandleCallback.class));
    }

    @Test
    public void providerCreatesAttachedInstance() {
        h2Extension.getJdbi().useHandle(handle -> {
            ProviderDao dao = handle.attach(ProviderDao.class);

            assertThat(dao).isExactlyInstanceOf(ProviderDaoImpl.class);
            dao.insert(1, "Alice");
            assertThat(dao.list()).extracting(Something::getName).containsExactly("Alice");
            assertThat(dao.count()).isEqualTo(1);
            assertThat(dao.getHandle()).isSameAs(handle);
        });
    }

    @Test
    public void providerCreatesOnDemandInstance() {
        ProviderDao dao = h2Extension.getJdbi().onDemand(ProviderDao.class);

        assertThat(dao).isExactlyInstanceOf(ProviderDaoImpl.OnDemand.class);
        dao.insert(2, "Bob");
        assertThat(dao.count()).isEqualTo(1);
    }

    private static Map<Class<?>, GeneratedSqlObjectProvider> loadProviders() {
        return StreamSupport.stream(ServiceLoader.load(GeneratedSqlObjectProvider.class).spliterator(), false)
                .collect(Collectors.toMap(GeneratedSqlObjectProvider::extensionType, Function.identity()));
    }

    @GenerateSqlObject
    public interface ProviderDao extends SqlObject {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(int id, String name);

        @SqlQuery("select * from something order by id")
        List<Something> list();

        default int count() {
            return list().size();
        }
    }
}

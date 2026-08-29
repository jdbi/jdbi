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

import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.GenerateSqlObject;
import org.jdbi.v3.sqlobject.GeneratedSqlObjectProvider;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
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
        assertThat(loadProviders()).containsOnlyKeys(
                ProviderDao.class,
                ArrayBindingTest.BazDao.class,
                NonpublicSubclassTest.AbstractClassDao.class,
                NonpublicSubclassTest.InterfaceDao.class);
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

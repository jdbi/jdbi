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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.config.ConfigCustomizer;
import org.jdbi.v3.core.extension.ConfigCustomizerFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionConfigCustomizerTest {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());
    Jdbi jdbi;

    interface SomethingDao {

        @SqlUpdate("INSERT INTO something (id, name) VALUES (:s.id, :s.name)")
        int insert(@BindBean("s") Something s);

        @SqlQuery("SELECT id, name FROM something WHERE id = ?")
        Optional<Something> findSomething(int id);
    }

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();

        // tag::register-global[]
        jdbi.configure(Extensions.class, e ->
                e.registerConfigCustomizerFactory(new SomethingMapperConfigCustomizerFactory())); // <1>
        // end::register-global[]

        jdbi.useExtension(SomethingDao.class, dao -> {
            dao.insert(new Something(1, "apple"));
            dao.insert(new Something(2, "banana"));
            dao.insert(new Something(3, "coconut"));
            dao.insert(new Something(4, "date"));
        });

    }


    @Test
    public void testFindById() {
        Optional<Something> result = jdbi.withExtension(SomethingDao.class, dao -> dao.findSomething(1));

        assertThat(result)
                .isPresent()
                .contains(new Something(1, "apple"));

    }

    @Test
    public void testHandleFindById() {

        try (Handle handle = jdbi.open()) {
            SomethingDao dao = handle.attach(SomethingDao.class);
            Optional<Something> result = dao.findSomething(1);

            assertThat(result)
                    .isPresent()
                    .contains(new Something(1, "apple"));
        }
    }

    // tag::config-customizer[]
    static class SomethingMapperConfigCustomizerFactory implements ConfigCustomizerFactory {

        @Override
        public Collection<ConfigCustomizer> forExtensionType(Class<?> extensionType) {
            return Collections.singleton(
                    config -> config.get(RowMappers.class).register(new SomethingMapper()) // <2>
            );
        }
    }
    // end::config-customizer[]

    static class SomethingMapper implements RowMapper<Something> {

        @Override
        public Something map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Something(
                    rs.getInt("id"),
                    rs.getString("name")
            );
        }
    }
}

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
import java.util.Optional;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionFrameworkTest {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());
    Jdbi jdbi;

    // tag::dao[]
    interface SomethingDao {
        @SqlUpdate("INSERT INTO something (id, name) VALUES (:s.id, :s.name)")
        int insert(@BindBean("s") Something s);

        @SqlQuery("SELECT id, name FROM something WHERE id = ?")
        Optional<Something> findSomething(int id);
    }
    // end::dao[]

    static class SomethingMapper implements RowMapper<Something> {

        @Override
        public Something map(ResultSet rs, StatementContext ctx) throws SQLException {
            return new Something(
                    rs.getInt("id"),
                    rs.getString("name")
            );
        }
    }

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();

        jdbi.registerRowMapper(new SomethingMapper());

        // tag::use[]
        jdbi.useExtension(SomethingDao.class, dao -> {
            dao.insert(new Something(1, "apple"));
        });
        // end::use[]
    }


    @Test
    public void testFindById() {
        // tag::with[]
        Optional<Something> result = jdbi.withExtension(SomethingDao.class, dao -> dao.findSomething(1));
        // end::with[]

        assertThat(result)
                .isPresent()
                .contains(new Something(1, "apple"));

    }

    @Test
    public void testHandleFindById() {

        try (Handle handle = jdbi.open()) {
            // tag::handle_attach[]
            SomethingDao dao = handle.attach(SomethingDao.class);
            Optional<Something> result = dao.findSomething(1);
        // end::handle_attach[]

            assertThat(result)
                .isPresent()
                .contains(new Something(1, "apple"));
        }
    }
}

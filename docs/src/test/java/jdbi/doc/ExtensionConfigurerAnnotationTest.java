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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.extension.SimpleExtensionConfigurer;
import org.jdbi.core.extension.annotation.UseExtensionConfigurer;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.mapper.RowMappers;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionConfigurerAnnotationTest {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());
    Jdbi jdbi;

    //tag::dao[]
    interface SomethingDao {

        @SqlUpdate("INSERT INTO something (id, name) VALUES (:s.id, :s.name)")
        int insert(@BindBean("s") Something s);

        @SqlQuery("SELECT id, name FROM something WHERE id = ?")
        @RegisterSomethingMapper // <2>
        Optional<Something> findSomething(int id);
    }
    //end::dao[]

    @BeforeEach
    void setUp() {
        this.jdbi = h2Extension.getJdbi();

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

    //tag::annotation[]
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    @UseExtensionConfigurer(value = SomethingMapperExtensionConfigurer.class) // <1>
    @interface RegisterSomethingMapper {}
    //end::annotation[]

    //tag::extension-configurer[]
    public static class SomethingMapperExtensionConfigurer extends SimpleExtensionConfigurer { // <3>

        @Override
        public void configure(ConfigRegistry config, Annotation annotation, Class<?> extensionType) {
            config.get(RowMappers.class).register(new SomethingMapper());
        }
    }
    //end::extension-configurer[]

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

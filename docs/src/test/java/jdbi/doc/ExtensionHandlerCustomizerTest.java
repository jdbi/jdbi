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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.AttachedExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandler;
import org.jdbi.v3.core.extension.ExtensionHandlerCustomizer;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.mapper.RowMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionHandlerCustomizerTest {

    public static final Logger LOG = LoggerFactory.getLogger(ExtensionHandlerCustomizerTest.class);

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());
    Jdbi jdbi;

    interface SomethingDao {

        default void initialize() {
            insert(new Something(1, "apple"));
            insert(new Something(2, "banana"));
            insert(new Something(3, "coconut"));
            insert(new Something(4, "date"));
        }

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
                e.registerHandlerCustomizer(new LoggingExtensionHandlerCustomizer())); // <1>
        // end::register-global[]

        jdbi.registerRowMapper(new SomethingMapper());

        jdbi.useExtension(SomethingDao.class, SomethingDao::initialize);
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

    // tag::extension-handler-customizer[]
    static class LoggingExtensionHandlerCustomizer implements ExtensionHandlerCustomizer {

        @Override
        public ExtensionHandler customize(ExtensionHandler handler, Class<?> extensionType, Method method) {
            return (config, target) -> {
                AttachedExtensionHandler delegate = handler.attachTo(config, target);
                return (handleSupplier, args) -> {
                    LOG.info(format("Entering %s on %s", method, extensionType.getSimpleName()));
                    try {
                        return delegate.invoke(handleSupplier, args);
                    } finally {
                        LOG.info("Leaving {} on {}", method, extensionType.getSimpleName());
                    }
                };
            };
        }
    }
    // end::extension-handler-customizer[]

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

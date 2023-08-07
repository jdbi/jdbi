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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.Extensions;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.DONT_USE_PROXY;

class DontUseProxyExtensionFactoryTest {

    @RegisterExtension
    JdbiExtension h2Extension = JdbiExtension.h2()
            .withInitializer(TestingInitializers.something())
            .withPlugin(new SqlObjectPlugin());

    Jdbi jdbi;

    //tag::abstract-class[]
    abstract static class AbstractDao {

        public abstract Optional<Something> findSomething(int id);

        Something findFirstSomething() {
            return findSomething(1).orElseGet(Assertions::fail);
        }
    }
    //end::abstract-class[]

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
        jdbi.configure(Extensions.class, e -> e.register(new DontUseProxyExtensionFactory()));

        jdbi.useHandle(handle -> {
            handle.execute("INSERT INTO something (id, name) VALUES (1, 'apple')");
            handle.execute("INSERT INTO something (id, name) VALUES (2, 'banana')");
        });
    }

    @Test
    public void testFindById() {
        Something result = jdbi.withExtension(AbstractDao.class, AbstractDao::findFirstSomething);

        assertThat(result).isEqualTo(new Something(1, "apple"));
    }

    static class DaoImpl extends AbstractDao {

        private final HandleSupplier handleSupplier;

        DaoImpl(HandleSupplier handleSupplier) {
            this.handleSupplier = handleSupplier;
        }

        @Override
        public Optional<Something> findSomething(int id) {
            try (Query query = handleSupplier.getHandle().createQuery("SELECT * FROM something WHERE id = :id")) {
                return query.bind("id", id)
                        .map(new SomethingMapper())
                        .findFirst();
            }
        }
    }

    // tag::abstract-class-factory[]
    public static class DontUseProxyExtensionFactory implements ExtensionFactory {

        @Override
        public Set<FactoryFlag> getFactoryFlags() {
            return EnumSet.of(DONT_USE_PROXY); // <1>
        }

        @Override
        public boolean accepts(Class<?> extensionType) {
            return extensionType == AbstractDao.class;
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
            return extensionType.cast(new DaoImpl(handleSupplier)); // <2>
        }
    }
    // end::abstract-class-factory[]
}

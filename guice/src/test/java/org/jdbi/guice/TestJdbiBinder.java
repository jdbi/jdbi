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
package org.jdbi.guice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Jdbi;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMappers;
import org.jdbi.core.mapper.RowMapper;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.guice.util.GuiceTestSupport;
import org.jdbi.guice.util.MyString;
import org.jdbi.guice.util.MyString.MyStringColumnMapper;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.assertj.core.api.Assertions.assertThat;

public class TestJdbiBinder {

    @RegisterExtension
    public EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults()
        .withDatabasePreparer(ds -> GuiceTestSupport.executeSql(ds,
            "DROP TABLE IF EXISTS binder_test",
            "CREATE TABLE binder_test (i INT, u UUID, s VARCHAR, t timestamp with time zone default current_timestamp)"
        )).build();

    @Inject
    @BinderTest
    public Jdbi jdbi = null;

    @BeforeEach
    public void setUp() throws Exception {
        Module testModule = new AbstractJdbiDefinitionModule(BinderTest.class) {
            @Override
            public void configureJdbi() {
                bindPlugin().toInstance(PostgresPlugin.noUnqualifiedHstoreBindings());
                bindPlugin().toInstance(new SqlObjectPlugin());

                // pass testColumnMapper
                bindColumnMapper().to(MyStringColumnMapper.class).in(Scopes.SINGLETON);

                // pass testRowMapper
                bindRowMapper().to(BinderTestRow.Mapper.class).in(Scopes.SINGLETON);
            }
        };

        DataSource ds = pg.createDataSource();
        Injector inj = GuiceTestSupport.createTestInjector(
            binder -> binder.bind(DataSource.class).annotatedWith(BinderTest.class).toInstance(ds),
            testModule
        );

        inj.injectMembers(this);

        assertThat(jdbi).isNotNull();

        populateDb(jdbi);
    }

    private void populateDb(Jdbi db) {
        for (int c = 0; c < 100; c++) {
            final int i = c;
            final String s = "xxx" + c;
            final UUID u = UUID.randomUUID();

            int result = db.withExtension(Dao.class, dao -> dao.createRow(i, u, s));
            assertThat(result).isOne();
        }
    }

    @Test
    public void testSimple() {
        assertThat(jdbi.withExtension(Dao.class, Dao::countRows).intValue()).isEqualTo(100);
    }

    @Test
    public void testColumnMapper() {
        List<MyString> uuids = jdbi.withExtension(Dao.class, Dao::getMyStrings);
        assertThat(uuids).hasSize(100);
    }

    @Test
    public void testRowMapper() {
        List<BinderTestRow> rows = jdbi.withExtension(Dao.class, Dao::getRows);

        for (int i = 0; i < 100; i++) {
            assertThat(i).isEqualTo(rows.get(i).getI());
        }
    }

    //

    @Retention(RUNTIME)
    @Target({FIELD, PARAMETER, METHOD})
    @javax.inject.Qualifier
    @jakarta.inject.Qualifier
    public @interface BinderTest {}

    public interface Dao {

        @SqlUpdate("INSERT INTO binder_test (i, u, s) VALUES (:i, :u, :s)")
        int createRow(int i, UUID u, String s);

        @SqlQuery("SELECT COUNT(1) FROM binder_test")
        int countRows();

        @SqlQuery("SELECT u from binder_test")
        List<MyString> getMyStrings();

        @SqlQuery("SELECT * from binder_test")
        List<BinderTestRow> getRows();
    }

    public static class BinderTestRow {

        private final int i;
        private final String s;
        private final MyString u;
        private final OffsetDateTime t;

        public BinderTestRow(int i, String s, MyString u, OffsetDateTime t) {
            this.i = i;
            this.s = s;
            this.u = u;
            this.t = t;
        }

        public int getI() {
            return i;
        }

        public String getS() {
            return s;
        }

        public MyString getU() {
            return u;
        }

        public OffsetDateTime getT() {
            return t;
        }

        public static class Mapper implements RowMapper<BinderTestRow> {

            @Inject
            public Mapper() {}

            private ColumnMapper<MyString> myStringMapper;

            @Override
            public BinderTestRow map(ResultSet rs, StatementContext ctx) throws SQLException {
                return new BinderTestRow(
                    rs.getInt("i"),
                    rs.getString("s"),
                    myStringMapper.map(rs, "u", ctx),
                    rs.getObject("t", OffsetDateTime.class));
            }

            @Override
            public void init(ConfigRegistry registry) {
                myStringMapper = registry.get(ColumnMappers.class).findFor(MyString.class).orElseThrow(IllegalStateException::new);
            }
        }
    }

}

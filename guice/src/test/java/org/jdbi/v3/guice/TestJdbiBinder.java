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
package org.jdbi.v3.guice;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.sql.DataSource;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.opentable.db.postgres.junit.EmbeddedPostgresRules;
import com.opentable.db.postgres.junit.SingleInstancePostgresRule;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.guice.MyString.MyStringColumnMapper;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestJdbiBinder {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    @Inject
    @BinderTest
    public Jdbi jdbi = null;

    @Before
    public void setUp() {
        Module testModule = new AbstractJdbiModule(BinderTest.class) {
            @Override
            protected void configureJdbi() {
                bindPlugin().toInstance(PostgresPlugin.noUnqualifiedHstoreBindings());
                bindPlugin().toInstance(new SqlObjectPlugin());

                // pass testColumnMapper
                bindColumnMapper().to(MyStringColumnMapper.class).in(Scopes.SINGLETON);

                // pass testRowMapper
                bindRowMapper().to(BinderTestRow.Mapper.class).in(Scopes.SINGLETON);
            }
        };

        Injector inj = Guice.createInjector(Stage.PRODUCTION,
            testModule,
            binder -> binder.bind(DataSource.class).annotatedWith(BinderTest.class).toInstance(pg.getEmbeddedPostgres().getPostgresDatabase()),
            Binder::disableCircularProxies,
            Binder::requireExplicitBindings,
            Binder::requireExactBindingAnnotations,
            Binder::requireAtInjectOnConstructors);

        inj.injectMembers(this);

        assertNotNull(jdbi);
        createDb();
    }

    private void createDb() {
        jdbi.inTransaction(h -> {
            h.execute("DROP TABLE IF EXISTS binder_test");
            h.execute("CREATE TABLE binder_test (i INT, u UUID, s VARCHAR, t timestamp with time zone default current_timestamp)");
            return null;
        });

        for (int c = 0; c < 100; c++) {
            final int i = c;
            final String s = "xxx" + c;
            final UUID u = UUID.randomUUID();

            int result = jdbi.withExtension(Dao.class, dao -> dao.createRow(i, u, s));
            assertEquals(1, result);
        }

    }

    @Test
    public void testSimple() {
        assertEquals(100, jdbi.withExtension(Dao.class, Dao::countRows).intValue());
    }

    @Test
    public void testColumnMapper() {
        List<MyString> uuids = jdbi.withExtension(Dao.class, Dao::getMyStrings);
        assertEquals(100, uuids.size());
    }

    @Test
    public void testRowMapper() {
        List<BinderTestRow> rows = jdbi.withExtension(Dao.class, Dao::getRows);

        for (int i = 0; i < 100; i++) {
            assertEquals(i, rows.get(i).getI());
        }
    }

    //

    @Retention(RUNTIME)
    @Target({FIELD, PARAMETER, METHOD})
    @Qualifier
    public @interface BinderTest {}

    public interface Dao {

        @SqlUpdate("INSERT INTO binder_test (i, u, s) VALUES (:i, :u, :s)")
        int createRow(@Bind("i") int i, @Bind("u") UUID u, @Bind("s") String s);

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

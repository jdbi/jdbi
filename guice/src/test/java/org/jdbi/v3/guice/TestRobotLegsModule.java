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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import com.google.inject.Injector;
import com.google.inject.name.Names;
import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.guice.util.GuiceTestSupport;
import org.jdbi.v3.guice.util.JsonCodec;
import org.jdbi.v3.guice.util.Right;
import org.jdbi.v3.guice.util.table.Table;
import org.jdbi.v3.guice.util.table.Table.TableMapper;
import org.jdbi.v3.guice.util.table.TableDao;
import org.jdbi.v3.guice.util.table.TableModule;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

public class TestRobotLegsModule {

    @RegisterExtension
    public EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults()
        .withPreparer(ds -> GuiceTestSupport.executeSql(ds,
            "CREATE EXTENSION IF NOT EXISTS hstore",
            "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"",
            "DROP TABLE IF EXISTS left_data",
            "CREATE TABLE left_data (u UUID, s VARCHAR, j JSONB)",
            "DROP TABLE IF EXISTS right_data",
            "CREATE TABLE right_data (u UUID, s VARCHAR, j JSONB)"
        )).build();

    @Inject
    @Named("left")
    private TableDao leftDao;

    @Inject
    @Named("left")
    private Jdbi leftJdbi;

    @Inject
    @Right
    private TableDao rightDao;

    @Inject
    @Right
    private Jdbi rightJdbi;

    @BeforeEach
    public void setUp() throws Exception {
        DataSource dsLeft = pg.createDataSource();
        DataSource dsRight = pg.createDataSource();

        Injector inj = GuiceTestSupport.createTestInjector(
            new JdbiPluginModule(),
            new JdbiMapperModule(),

            binder -> binder.bind(DataSource.class).annotatedWith(Names.named("left")).toInstance(dsLeft),
            new TableModule(Names.named("left"), "left_data"),

            binder -> binder.bind(DataSource.class).annotatedWith(Right.class).toInstance(dsRight),
            new TableModule(Right.class, "right_data"));

        inj.injectMembers(this);

        assertNotSame(leftJdbi, rightJdbi);
        assertNotSame(leftDao, rightDao);
    }

    @Test
    public void testRobotLegs() {
        Table leftTable = Table.randomTable();
        assertEquals(1, leftDao.insert(leftTable));
        List<Table> left = leftDao.select();
        assertEquals(1, left.size());
        assertEquals(leftTable, left.get(0));

        Table rightTable = Table.randomTable();
        assertEquals(1, rightDao.insert(rightTable));
        List<Table> right = rightDao.select();
        assertEquals(1, right.size());
        assertEquals(rightTable, right.get(0));

        assertNotEquals(leftTable, rightTable);
    }

    static class JdbiPluginModule extends AbstractJdbiConfigurationModule {

        @Override
        public void configureJdbi() {
            bindPlugin().toInstance(PostgresPlugin.noUnqualifiedHstoreBindings());
            bindPlugin().toInstance(new SqlObjectPlugin());
        }
    }

    static class JdbiMapperModule extends AbstractJdbiConfigurationModule {

        @Override
        public void configureJdbi() {
            bindCodec(JsonCodec.TYPE).to(JsonCodec.class);
            bindRowMapper().to(TableMapper.class);
        }
    }
}


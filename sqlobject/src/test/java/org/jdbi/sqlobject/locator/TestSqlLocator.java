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
package org.jdbi.sqlobject.locator;

import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.config.JdbiConfig;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.SqlObjects;
import org.jdbi.sqlobject.config.RegisterBeanMapper;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlLocator {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new SqlObjectPlugin())
        .withInitializer((ds, h) -> {
            h.execute("create table something (id int, name text)");

            h.execute("insert into something (id, name) values (?, ?)", 2, "Alice");
            h.execute("insert into something (id, name) values (?, ?)", 1, "Bob");
        })
        .withConfig(b -> b.configure(SqlObjects.class, c -> c.sqlLocator(
            (type, method, config) -> config.get(TestConfig.class).sql())));

    @Test
    public void testLocateConfigDriven() throws Exception {
        try (Handle h = pgExtension.openWithConfig(
                b -> b.configure(TestConfig.class, c -> c.sql("select * from something order by id")))) {
            assertThat(h.attach(TestDao.class).list())
                .containsExactly(new Something(1, "Bob"), new Something(2, "Alice"));
        }

        try (Handle h = pgExtension.openWithConfig(
                b -> b.configure(TestConfig.class, c -> c.sql("select * from something order by name")))) {
            assertThat(h.attach(TestDao.class).list())
                .containsExactly(new Something(2, "Alice"), new Something(1, "Bob"));
        }
    }

    public static class TestConfig implements JdbiConfig<TestConfig> {
        private final String sql;

        public TestConfig() {
            this(null);
        }

        private TestConfig(String sql) {
            this.sql = sql;
        }

        String sql() {
            return sql;
        }

        TestConfig sql(String sql) {
            return new TestConfig(sql);
        }
    }

    @RegisterBeanMapper(Something.class)
    public interface TestDao {
        @SqlQuery
        List<Something> list();
    }
}

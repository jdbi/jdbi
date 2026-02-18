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
import org.jdbi.core.Jdbi;
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
        });

    @Test
    public void testLocateConfigDriven() throws Exception {
        Jdbi jdbi = pgExtension.getJdbi();

        jdbi.getConfig(SqlObjects.class).setSqlLocator(
            (type, method, config) -> config.get(TestConfig.class).sql);

        jdbi.useHandle(h -> {
            h.getConfig(TestConfig.class).sql = "select * from something order by id";
            assertThat(jdbi.withExtension(TestDao.class, TestDao::list))
                .containsExactly(new Something(1, "Bob"), new Something(2, "Alice"));
        });

        jdbi.useHandle(h -> {
            h.getConfig(TestConfig.class).sql = "select * from something order by name";
            assertThat(jdbi.withExtension(TestDao.class, TestDao::list))
                .containsExactly(new Something(2, "Alice"), new Something(1, "Bob"));
        });
    }

    public static class TestConfig implements JdbiConfig<TestConfig> {
        private String sql;

        public TestConfig() {}

        private TestConfig(TestConfig that) {
            this.sql = that.sql;
        }

        @Override
        public TestConfig createCopy() {
            return new TestConfig(this);
        }
    }

    @RegisterBeanMapper(Something.class)
    public interface TestDao {
        @SqlQuery
        List<Something> list();
    }
}

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
package org.jdbi.v3.sqlobject.locator;

import java.util.List;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.SqlObjects;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSqlLocator {
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testLocateConfigDriven() {
        Jdbi jdbi = dbRule.getJdbi();
        jdbi.useHandle(h -> {
            h.execute("create table something (id int, name text)");

            h.execute("insert into something (id, name) values (?, ?)", 2, "Alice");
            h.execute("insert into something (id, name) values (?, ?)", 1, "Bob");
        });

        jdbi.getConfig(SqlObjects.class).setSqlLocator(
            (type, method, config) -> config.get(TestConfig.class).sql);

        jdbi.getConfig(TestConfig.class).sql = "select * from something order by id";
        assertThat(jdbi.withExtension(TestDao.class, TestDao::list))
            .containsExactly(new Something(1, "Bob"), new Something(2, "Alice"));

        jdbi.getConfig(TestConfig.class).sql = "select * from something order by name";
        assertThat(jdbi.withExtension(TestDao.class, TestDao::list))
            .containsExactly(new Something(2, "Alice"), new Something(1, "Bob"));
    }

    public static class TestConfig implements JdbiConfig<TestConfig> {
        String sql;

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

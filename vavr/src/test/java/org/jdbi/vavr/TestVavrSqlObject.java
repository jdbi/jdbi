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
package org.jdbi.vavr;

import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import io.vavr.control.Option;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.sqlobject.SqlObjectPlugin;
import org.jdbi.sqlobject.config.RegisterBeanMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestVavrSqlObject {

    private static final Something ERIC = new Something(1, "eric");
    private static final Something BRIAN = new Something(2, "brian");

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugins(new SqlObjectPlugin(), new VavrPlugin());

    @BeforeEach
    public void createTestData() {
        Handle handle = pgExtension.getSharedHandle();
        handle.execute("create table something (id integer primary key, name varchar(50))");

        handle.createUpdate("insert into something (id, name) values (1, 'eric')").execute();
        handle.createUpdate("insert into something (id, name) values (2, 'brian')").execute();
    }

    @Test
    public void testGetOptionShouldReturnCorrectRowUsingExtension() {
        List<Something> result = pgExtension.getJdbi()
            .withExtension(Dao.class, dao -> dao.selectByOptionName(Option.of("eric")));

        assertThat(result).hasSize(1).containsExactly(ERIC);
    }

    @Test
    public void testGetOptionEmptyShouldReturnAllRowsUsingExtension() {
        List<Something> result = pgExtension.getJdbi()
            .withExtension(Dao.class, dao -> dao.selectByOptionName(Option.none()));

        assertThat(result).hasSize(2).containsExactly(ERIC, BRIAN);
    }

    interface Dao {

        @SqlQuery("select * from something where :name is null or name = :name order by id")
        @RegisterBeanMapper(Something.class)
        List<Something> selectByOptionName(@Bind Option<String> name);
    }
}

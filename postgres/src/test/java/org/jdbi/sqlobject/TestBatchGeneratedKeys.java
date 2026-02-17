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
package org.jdbi.sqlobject;

import java.util.Arrays;
import java.util.List;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.postgres.PostgresPlugin;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.BatchChunkSize;
import org.jdbi.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.testing.junit.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

// This test arguably should be in jdbi-sqlobject but it needs Postgres
// features to test generated keys
public class TestBatchGeneratedKeys {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
        .withPlugin(new SqlObjectPlugin())
        .withPlugin(new PostgresPlugin());
    private Handle handle;
    private UsesBatching b;

    @BeforeEach
    public void setUp() {
        handle = pgExtension.openHandle();
        handle.execute("create table something (id serial primary key, name varchar)");
        b = handle.attach(UsesBatching.class);
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testReturnKey() {
        long[] ids = b.insertNames("a", "b", "c", "d", "e");
        assertThat(ids).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testBeanReturn() {
        Something[] people = b.insertNamesToBean(Arrays.asList("a", "b", "c", "d", "e"));
        assertThat(people).hasSize(5);
        for (int i = 0; i < people.length; i++) {
            assertThat(people[i].getId()).isEqualTo(i + 1);
            assertThat(people[i].getName()).isEqualTo(nameByIndex(i));
        }
    }

    @Test
    public void testVarargsList() {
        List<Something> people = b.insertVarargs("a", "b", "c", "d", "e");
        assertThat(people).hasSize(5);
        for (int i = 0; i < people.size(); i++) {
            assertThat(people.get(i).getId()).isEqualTo(i + 1);
            assertThat(people.get(i).getName()).isEqualTo(nameByIndex(i));
        }
    }

    private String nameByIndex(int i) {
        return String.valueOf((char) ('a' + i));
    }

    @BatchChunkSize(2)
    @RegisterRowMapper(SomethingMapper.class)
    public interface UsesBatching {
        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long[] insertNames(@Bind("name") String... names);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        Something[] insertNamesToBean(@Bind("name") Iterable<String> names);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        List<Something> insertVarargs(@Bind("name") String... names);
    }
}

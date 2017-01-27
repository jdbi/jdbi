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
package org.jdbi.v3.sqlobject;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.rule.PgDatabaseRule;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.statement.BatchChunkSize;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

// This test arguably should be in jdbi-sqlobject but it needs Postgres
// features to test generated keys
public class TestBatchGeneratedKeys
{
    @Rule
    public PgDatabaseRule dbRule = new PgDatabaseRule()
            .withPlugin(new SqlObjectPlugin())
            .withPlugin(new PostgresPlugin());
    private Handle handle;
    private UsesBatching b;

    @Before
    public void setUp() throws Exception
    {
        handle = dbRule.openHandle();
        handle.execute("create table something (id serial primary key, name varchar)");
        b = handle.attach(UsesBatching.class);
    }

    @Test
    public void testReturnKey() throws Exception
    {
        long[] ids = b.insertNames("a", "b", "c", "d", "e");
        assertThat(ids).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    public void testBeanReturn() throws Exception
    {
        Something[] people = b.insertNamesToBean(Arrays.asList("a", "b", "c", "d", "e"));
        assertThat(people.length).isEqualTo(5);
        for (int i = 0; i < people.length; i++) {
            assertThat(people[i].getId()).isEqualTo(i + 1);
            assertThat(people[i].getName()).isEqualTo(nameByIndex(i));
        }
    }

    @Test
    public void testVarargsList() throws Exception
    {
        List<Something> people = b.insertVarargs("a", "b", "c", "d", "e");
        assertThat(people.size()).isEqualTo(5);
        for (int i = 0; i < people.size(); i++) {
            assertThat(people.get(i).getId()).isEqualTo(i + 1);
            assertThat(people.get(i).getName()).isEqualTo(nameByIndex(i));
        }
    }

    private String nameByIndex(int i) {
        return String.valueOf((char)('a' + i));
    }

    @BatchChunkSize(2)
    @RegisterRowMapper(SomethingMapper.class)
    public interface UsesBatching
    {
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

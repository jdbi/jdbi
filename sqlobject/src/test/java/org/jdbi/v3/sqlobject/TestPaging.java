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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import com.google.common.collect.ImmutableList;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.guava.GuavaCollectors;
import org.jdbi.v3.sqlobject.config.RegisterCollectorFactory;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPaging
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void pagingExample() throws Exception
    {
        Sql sql = handle.attach(Sql.class);

        int[] rs = sql.insert(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                              asList("Ami", "Brian", "Cora", "David", "Eric",
                                     "Fernando", "Greta", "Holly", "Inigo", "Joy",
                                     "Keith", "Lisa", "Molly"));

        assertThat(rs).hasSize(13).containsOnly(1);

        List<Something> page_one = sql.loadPage(-1, 5);
        assertThat(page_one).containsExactly(new Something(1, "Ami"),
                                             new Something(2, "Brian"),
                                             new Something(3, "Cora"),
                                             new Something(4, "David"),
                                             new Something(5, "Eric"));

        List<Something> page_two = sql.loadPage(page_one.get(page_one.size() - 1).getId(), 5);
        assertThat(page_two).containsExactly(new Something(6, "Fernando"),
                                             new Something(7, "Greta"),
                                             new Something(8, "Holly"),
                                             new Something(9, "Inigo"),
                                             new Something(10, "Joy"));
    }

    @RegisterRowMapper(SomethingMapper.class)
    @RegisterCollectorFactory(GuavaCollectors.Factory.class)
    public interface Sql
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] insert(@Bind("id") Iterable<Integer> ids, @Bind("name") Iterable<String> names);

        @SqlQuery("select id, name from something where id > :end_of_last_page order by id limit :size")
        ImmutableList<Something> loadPage(@Bind("end_of_last_page") int last, @Bind("size") int size);
    }
}

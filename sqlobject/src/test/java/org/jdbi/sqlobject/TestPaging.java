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

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.guava.GuavaPlugin;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;

public class TestPaging {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin()).withPlugin(new GuavaPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void pagingExample() {
        Sql sql = handle.attach(Sql.class);

        int[] rs = sql.insert(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                              asList("Ami", "Brian", "Cora", "David", "Eric",
                                     "Fernando", "Greta", "Holly", "Inigo", "Joy",
                                     "Keith", "Lisa", "Molly"));

        assertThat(rs).hasSize(13).containsOnly(1);

        List<Something> pageOne = sql.loadPage(-1, 5);
        assertThat(pageOne).containsExactly(new Something(1, "Ami"),
                                             new Something(2, "Brian"),
                                             new Something(3, "Cora"),
                                             new Something(4, "David"),
                                             new Something(5, "Eric"));

        List<Something> pageTwo = sql.loadPage(pageOne.get(pageOne.size() - 1).getId(), 5);
        assertThat(pageTwo).containsExactly(new Something(6, "Fernando"),
                                             new Something(7, "Greta"),
                                             new Something(8, "Holly"),
                                             new Something(9, "Inigo"),
                                             new Something(10, "Joy"));
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Sql {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] insert(@Bind("id") Iterable<Integer> ids, @Bind("name") Iterable<String> names);

        @SqlQuery("select id, name from something where id > :end_of_last_page order by id limit :size")
        ImmutableList<Something> loadPage(@Bind("end_of_last_page") int last, @Bind("size") int size);
    }
}

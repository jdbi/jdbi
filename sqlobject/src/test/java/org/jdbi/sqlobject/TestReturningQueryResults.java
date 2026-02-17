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

import java.util.Iterator;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReturningQueryResults {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();
    }

    @Test
    public void testSingleValue() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        h2Extension.getJdbi().useExtension(Spiffy.class, spiffy -> {
            Something s = spiffy.findById(7);
            assertThat(s.getName()).isEqualTo("Tim");
        });
    }

    @Test
    public void testIterator() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");
        handle.execute("insert into something (id, name) values (3, 'Diego')");

        h2Extension.getJdbi().useExtension(Spiffy.class, spiffy -> {
            Iterator<Something> itty = spiffy.findByIdRange(2, 10);
            assertThat(itty).toIterable().containsOnlyOnce(new Something(7, "Tim"), new Something(3, "Diego"));
        });
    }

    @Test
    public void testList() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");
        handle.execute("insert into something (id, name) values (3, 'Diego')");

        h2Extension.getJdbi().useExtension(Spiffy.class, spiffy -> {
            List<Something> all = spiffy.findTwoByIds(3, 7);
            assertThat(all).containsOnlyOnce(new Something(7, "Tim"), new Something(3, "Diego"));
        });
    }

    public interface Spiffy {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something findById(@Bind("id") int id);

        @SqlQuery("select id, name from something where id >= :from and id <= :to")
        @UseRowMapper(SomethingMapper.class)
        Iterator<Something> findByIdRange(@Bind("from") int from, @Bind("to") int to);

        @SqlQuery("select id, name from something where id = :first or id = :second")
        @UseRowMapper(SomethingMapper.class)
        List<Something> findTwoByIds(@Bind("first") int from, @Bind("second") int to);

    }
}

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

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.result.ResultIterable;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestReturningQuery {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();

    }

    @Test
    public void testWithRegisteredMapper() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        dbRule.getJdbi().useExtension(Spiffy.class, spiffy -> {
            Something s = spiffy.findById(7).findOnly();

            assertThat(s.getName()).isEqualTo("Tim");
        });
    }

    @Test
    public void testWithExplicitMapper() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        dbRule.getJdbi().useExtension(Spiffy2.class, spiffy -> {
            Something s = spiffy.findByIdWithExplicitMapper(7).findOnly();

            assertThat(s.getName()).isEqualTo("Tim");
        });
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Spiffy {
        @SqlQuery("select id, name from something where id = :id")
        ResultIterable<Something> findById(@Bind("id") int id);
    }

    public interface Spiffy2 {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        ResultIterable<Something> findByIdWithExplicitMapper(@Bind("id") int id);
    }
}

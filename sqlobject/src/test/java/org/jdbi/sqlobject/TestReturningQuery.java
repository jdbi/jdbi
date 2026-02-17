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

import org.jdbi.core.Handle;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.result.ResultIterable;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.testing.junit5.JdbiExtension;
import org.jdbi.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestReturningQuery {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @BeforeEach
    public void setUp() {
        handle = h2Extension.getSharedHandle();

    }

    @Test
    public void testWithRegisteredMapper() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        h2Extension.getJdbi().useExtension(Spiffy.class, spiffy -> {
            Something s = spiffy.findById(7).one();

            assertThat(s.getName()).isEqualTo("Tim");
        });
    }

    @Test
    public void testWithExplicitMapper() {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        h2Extension.getJdbi().useExtension(Spiffy2.class, spiffy -> {
            Something s = spiffy.findByIdWithExplicitMapper(7).one();

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

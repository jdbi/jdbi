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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConsumer {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    Spiffy dao;
    List<Something> expected;

    @BeforeEach
    public void setup() {
        final Something one = new Something(3, "foo");
        final Something two = new Something(4, "bar");
        final Something thr = new Something(5, "baz");

        dao = h2Extension.getSharedHandle().attach(Spiffy.class);
        dao.insert(one);
        dao.insert(thr);
        dao.insert(two);

        expected = Arrays.asList(thr, two, one);
    }

    @Test
    public void consumeEach() {
        final List<Something> results = new ArrayList<>();
        dao.forEach(results::add);
        assertThat(results).containsExactlyElementsOf(expected);
    }

    @Test
    public void consumeIterator() {
        final List<Something> results = new ArrayList<>();
        dao.consumeIterator(iter -> iter.forEachRemaining(results::add));
        assertThat(results).containsExactlyElementsOf(expected);
    }

    @Test
    public void consumeStream() {
        final List<Something> results = new ArrayList<>();
        dao.consumeStream(stream -> stream.forEach(results::add));
        assertThat(results).containsExactlyElementsOf(expected);
    }

    public interface Spiffy {
        @SqlQuery("select id, name from something order by id desc")
        @UseRowMapper(SomethingMapper.class)
        void forEach(Consumer<Something> consumer);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeIterator(Consumer<Iterator<Something>> consumer);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeStream(Consumer<Stream<Something>> consumer);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);
    }
}

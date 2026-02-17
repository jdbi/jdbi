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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.qualifier.Reversed;
import org.jdbi.core.qualifier.ReversedStringArgumentFactory;
import org.jdbi.core.qualifier.ReversedStringMapper;
import org.jdbi.core.result.ResultIterator;
import org.jdbi.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.sqlobject.config.RegisterColumnMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.statement.UseRowMapper;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    public void consumeMultiStream() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(MultiConsumerDao.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void consumeIterable() {
        final List<Something> results = new ArrayList<>();
        final List<Something> results2 = new ArrayList<>();
        dao.consumeIterable(it -> {
            // can read it once
            it.forEach(results::add);

            // throws exception on second attempt
            assertThatThrownBy(() -> it.forEach(results2::add))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("stream has already been operated upon or closed");
        });
        assertThat(results).containsExactlyElementsOf(expected);
        assertThat(results2).isEmpty();
    }

    @Test
    public void consumeIterableIterator() {
        final List<Something> results = new ArrayList<>();
        final List<Something> results2 = new ArrayList<>();
        dao.consumeIterable(it -> {
            // can read it once
            it.iterator().forEachRemaining(results::add);

            // throws exception on second attempt
            assertThatThrownBy(() -> it.iterator().forEachRemaining(results2::add))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("stream has already been operated upon or closed");
        });
        assertThat(results).containsExactlyElementsOf(expected);
        assertThat(results2).isEmpty();
    }

    @Test
    public void testIteratorPartialConsumeOk() {
        final List<Something> results = new ArrayList<>();
        dao.consumeIterator(iter -> results.add(iter.next()));
        assertThat(results).containsExactly(expected.get(0));
    }

    @Test
    public void testStreamPartialConsumeOk() {
        final List<Something> results = new ArrayList<>();
        dao.consumeStream(stream -> results.add(stream.findFirst().orElse(null)));
        assertThat(results).containsExactly(expected.get(0));
    }

    @Test
    public void testSubclassIteratorFails() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(ResultIteratorDao.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSubclassStreamFails() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(SpecialStreamDao.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testConcreteStreamFails() {
        assertThatThrownBy(() -> h2Extension.getSharedHandle().attach(SomethingStreamDao.class)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testAnnotation() {
        List<String> result = new ArrayList<>();

        dao.streamReverse(s -> s.forEach(result::add));
        assertThat(result).containsExactly("zab", "rab", "oof");
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory.class)
    @RegisterColumnMapper(ReversedStringMapper.class)
    public interface Spiffy {
        @SqlQuery("select id, name from something order by id desc")
        @UseRowMapper(SomethingMapper.class)
        void forEach(Consumer<Something> consumer);

        @SqlQuery("select name from something order by id desc")
        void streamReverse(@Reversed Consumer<Stream<String>> consumer);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeIterator(Consumer<Iterator<Something>> consumer);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeStream(Consumer<Stream<Something>> consumer);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeIterable(Consumer<Iterable<Something>> consumer);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);
    }

    public interface ResultIteratorDao {
        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeIterator(Consumer<ResultIterator<Something>> consumer);
    }

    public interface SomethingStream extends Stream<Something> {}

    public interface SomethingStreamDao {

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeStream(Consumer<SomethingStream> consumer);

    }

    public interface SpecialStream<T> extends Stream<T> {}

    public interface SpecialStreamDao {

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeStream(Consumer<SpecialStream<Something>> consumer);

    }

    public interface MultiConsumerDao {

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        void consumeMultiStream(Consumer<Stream<Something>> stream, Consumer<Iterator<Something>> iterator);
    }
}

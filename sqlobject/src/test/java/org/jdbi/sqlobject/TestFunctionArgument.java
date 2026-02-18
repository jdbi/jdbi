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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.qualifier.Reversed;
import org.jdbi.core.qualifier.ReversedStringArgumentFactory;
import org.jdbi.core.qualifier.ReversedStringMapper;
import org.jdbi.core.result.ResultIterator;
import org.jdbi.sqlobject.config.RegisterArgumentFactory;
import org.jdbi.sqlobject.config.RegisterColumnMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestFunctionArgument {

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
    public void mapIterator() {
        List<Something> results = dao.mapIterator(Lists::newArrayList);
        assertThat(results).containsExactlyElementsOf(expected);
    }

    @Test
    public void mapIterable() {
        List<Something> results = dao.mapIterable(Lists::newArrayList);
        assertThat(results).containsExactlyElementsOf(expected);
    }

    @Test
    public void mapStream() {
        List<Something> results = dao.mapStream(stream -> stream.collect(Collectors.toList()));
        assertThat(results).containsExactlyElementsOf(expected);
    }

    @Test
    public void mapElement() {
        List<Something> results = dao.mapElement(4, stream -> stream.collect(Collectors.toList()));
        assertThat(results).containsOnly(new Something(4, "bar"));
    }

    @Test
    public void mapIterableTwice() {
        final List<Something> results2 = new ArrayList<>();
        final List<Something> results = dao.mapIterable(it -> {
            // can read it once
            List<Something> r = Lists.newArrayList(it);

            // throws exception on second attempt
            assertThatThrownBy(() -> it.forEach(results2::add))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stream has already been operated upon or closed");

            return r;
        });

        assertThat(results).containsExactlyElementsOf(expected);
        assertThat(results2).isEmpty();
    }

    @Test
    public void mapIterableIteratorTwice() {
        final List<Something> results2 = new ArrayList<>();
        final List<Something> results = dao.mapIterable(it -> {
            // can read it once
            List<Something> r = Lists.newArrayList(it.iterator());

            // throws exception on second attempt
            assertThatThrownBy(() -> it.iterator().forEachRemaining(results2::add))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("stream has already been operated upon or closed");

            return r;
        });

        assertThat(results).containsExactlyElementsOf(expected);
        assertThat(results2).isEmpty();
    }

    @Test
    public void testIteratorPartialConsumeOk() {
        final List<Something> results = dao.mapIterator(iter -> Collections.singletonList(iter.next()));
        assertThat(results).containsExactly(expected.get(0));
    }

    @Test
    public void testStreamPartialConsumeOk() {
        final List<Something> results = dao.mapStream(stream -> Collections.singletonList(stream.findFirst().orElse(null)));
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
        List<String> result = dao.streamReverse(s -> s.collect(Collectors.toList()));
        assertThat(result).containsExactly("zab", "rab", "oof");
    }

    @RegisterArgumentFactory(ReversedStringArgumentFactory.class)
    @RegisterColumnMapper(ReversedStringMapper.class)
    public interface Spiffy {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);

        @SqlQuery("select name from something order by id desc")
        List<String> streamReverse(@Reversed Function<Stream<String>, List<String>> function);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapStream(Function<Stream<Something>, List<Something>> function);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapIterator(Function<Iterator<Something>, List<Something>> function);

        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapIterable(Function<Iterable<Something>, List<Something>> function);

        @SqlQuery("select * from something where id = :id")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapElement(@Bind("id") int id, Function<Stream<Something>, List<Something>> function);
    }

    public interface ResultIteratorDao {
        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapIterator(Function<ResultIterator<Something>, List<Something>> consumer);
    }

    public interface SomethingStream extends Stream<Something> {}

    public interface SomethingStreamDao {
        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapStream(Function<SomethingStream, List<Something>> consumer);

    }

    public interface SpecialStream<T> extends Stream<T> {}

    public interface SpecialStreamDao {
        @SqlQuery("select id, name from something order by id desc")
        @RegisterRowMapper(SomethingMapper.class)
        List<Something> mapStream(Function<SpecialStream<Something>, List<Something>> consumer);
    }
}

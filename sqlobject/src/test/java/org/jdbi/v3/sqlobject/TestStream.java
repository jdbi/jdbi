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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.junit.Rule;
import org.junit.Test;

public class TestStream
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Test
    public void testReturnStream() throws Exception {
        Something one = new Something(3, "foo");
        Something two = new Something(4, "bar");
        Something thr = new Something(5, "baz");

        Spiffy dao = db.getDbi().open().attach(Spiffy.class);
        dao.insert(one);
        dao.insert(thr);
        dao.insert(two);

        final List<Something> results;
        try (final Stream<Something> stream = dao.stream()) {
            results = stream.collect(Collectors.toList());
        }

        assertEquals(ImmutableList.of(thr, two, one), results);
    }

    public interface Spiffy
    {
        @SqlQuery("select id, name from something order by id desc")
        @Mapper(SomethingMapper.class)
        Stream<Something> stream();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);
    }
}

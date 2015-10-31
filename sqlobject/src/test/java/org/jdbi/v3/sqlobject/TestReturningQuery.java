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

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Query;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.mixins.CloseMe;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestReturningQuery
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();

    }

    @Test
    public void testWithRegisteredMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        Spiffy spiffy = SqlObjectBuilder.attach(handle, Spiffy.class);

        Something s = spiffy.findById(7).findOnly();

        assertEquals("Tim", s.getName());
    }

    @Test
    public void testWithExplicitMapper() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");

        Spiffy2 spiffy = SqlObjectBuilder.attach(handle, Spiffy2.class);

        Something s = spiffy.findByIdWithExplicitMapper(7).findOnly();

        assertEquals("Tim", s.getName());
    }

    @RegisterMapper(SomethingMapper.class)
    public interface Spiffy extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        Query<Something> findById(@Bind("id") int id);
    }

    public interface Spiffy2 extends CloseMe
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        Query<Something> findByIdWithExplicitMapper(@Bind("id") int id);
    }
}

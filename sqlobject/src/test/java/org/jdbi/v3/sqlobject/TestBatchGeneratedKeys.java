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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.PGDatabaseRule;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBatchGeneratedKeys
{
    @Rule
    public PGDatabaseRule db = new PGDatabaseRule().withPlugin(new SqlObjectPlugin());
    private Handle handle;
    private UsesBatching b;

    @Before
    public void setUp() throws Exception
    {
        handle = db.openHandle();
        handle.execute("create table something (id serial primary key, name varchar)");
        b = handle.attach(UsesBatching.class);
    }

    @Test
    public void testReturnKey() throws Exception
    {
        long[] ids = b.insertNames("a", "b", "c", "d", "e");
        assertArrayEquals(new long[] { 1, 2, 3, 4, 5 }, ids);
    }

    @Test
    public void testBeanReturn() throws Exception
    {
        Something[] people = b.insertNamesToBean(Arrays.asList("a", "b", "c", "d", "e"));
        assertEquals(5, people.length);
        for (int i = 0; i < people.length; i++) {
            assertEquals(i + 1, people[i].getId());
            assertEquals(String.valueOf((char)('a' + i)), people[i].getName());
        }
    }

    @Test
    public void testVarargsList() throws Exception
    {
        List<Something> people = b.insertVarargs("a", "b", "c", "d", "e");
        assertEquals(5, people.size());
        for (int i = 0; i < people.size(); i++) {
            assertEquals(i + 1, people.get(i).getId());
            assertEquals(String.valueOf((char)('a' + i)), people.get(i).getName());
        }
    }


    @BatchChunkSize(2)
    @RegisterRowMapper(SomethingMapper.class)
    public interface UsesBatching
    {
        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long[] insertNames(@Bind("name") String... names);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        Something[] insertNamesToBean(@Bind("name") Iterable<String> names);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        List<Something> insertVarargs(@Bind("name") String... names);
    }
}

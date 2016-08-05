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

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBatchGeneratedKeys
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
    private Handle handle;
    private UsesBatching b;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
        b = handle.attach(UsesBatching.class);
    }

    @Test
    public void testReturnKey() throws Exception
    {
        long[] ids = b.insertNames("a", "b", "c", "d", "e");
        assertArrayEquals(new long[] { 0, 1, 2, 3, 4, 5 }, ids);
    }

    @Test
    public void testBeanReturn() throws Exception
    {
        Something[] people = b.insertNamesToBean(Arrays.asList("a", "b", "c", "d", "e"));
        assertEquals(5, people.length);
        for (int i = 0; i < people.length; i++) {
            assertEquals(i, people[i].getId());
            assertEquals(String.valueOf('a' + i), people[i].getName());
        }
    }

    @Test
    public void testVarargsList() throws Exception
    {
        List<Something> people = b.insertVarargs("a", "b", "c", "d", "e");
        assertEquals(5, people.size());
        for (int i = 0; i < people.size(); i++) {
            assertEquals(i, people.get(i).getId());
            assertEquals(String.valueOf('a' + i), people.get(i).getName());
        }
    }


    @BatchChunkSize(4)
    @RegisterRowMapper(SomethingMapper.class)
    public interface UsesBatching
    {
        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        long[] insertNames(@Bind("name") String... names);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        Something[] insertNamesToBean(@BindBean Iterable<String> names);

        @SqlBatch("insert into something (name) values (:name)")
        @GetGeneratedKeys
        List<Something> insertVarargs(String... names);
    }
}

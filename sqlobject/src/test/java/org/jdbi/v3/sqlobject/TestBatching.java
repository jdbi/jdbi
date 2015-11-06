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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestBatching
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
    public void testInsertSingleIterable() throws Exception
    {
        UsesBatching b = SqlObjectBuilder.attach(handle, UsesBatching.class);
        List<Something> to_insert = Arrays.asList(new Something(1, "Tom"), new Something(2, "Tatu"));
        int[] counts = b.insertBeans(to_insert);

        assertThat(counts.length, equalTo(2));
        assertThat(counts[0], equalTo(1));
        assertThat(counts[1], equalTo(1));

        assertThat(b.size(), equalTo(2));
    }

    @Test
    public void testInsertSingleIteratorNoTx() throws Exception
    {
        UsesBatching b = SqlObjectBuilder.attach(handle, UsesBatching.class);
        List<Something> to_insert = Arrays.asList(new Something(1, "Tom"), new Something(2, "Tatu"));
        int[] counts = b.insertBeansNoTx(to_insert.iterator());

        assertThat(counts.length, equalTo(2));
        assertThat(counts[0], equalTo(1));
        assertThat(counts[1], equalTo(1));

        assertThat(b.size(), equalTo(2));
    }

    @Test
    public void testBindConstantValue() throws Exception
    {
        UsesBatching b = SqlObjectBuilder.attach(handle, UsesBatching.class);
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);

        b.withConstantValue(ids, "Johan");

        assertThat(b.size(), equalTo(5));

        List<String> names = handle.createQuery("select distinct name from something")
                                   .mapTo(String.class)
                                   .list();
        assertThat(names, equalTo(Arrays.asList("Johan")));
    }

    @Test
    public void testZipping() throws Exception
    {
        UsesBatching b = SqlObjectBuilder.attach(handle, UsesBatching.class);
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        List<String> names = Arrays.asList("David", "Tim", "Mike");

        b.zipArgumentsTogether(ids, names);

        assertThat(b.size(), equalTo(3));

        List<String> ins_names = handle.createQuery("select distinct name from something order by name")
                                       .mapTo(String.class)
                                       .list();
        assertThat(ins_names, equalTo(Arrays.asList("David", "Mike", "Tim")));
    }

    @Test
    public void testChunkedBatching() throws Exception
    {
        UsesBatching b = SqlObjectBuilder.attach(handle, UsesBatching.class);
        List<Something> things = Arrays.asList(new Something(1, "Brian"),
                                               new Something(2, "Henri"),
                                               new Something(3, "Patrick"),
                                               new Something(4, "Robert"),
                                               new Something(5, "Maniax"));
        int[] counts = b.insertChunked(things);
        assertThat(counts.length, equalTo(5));
        for (int count : counts) {
            assertThat(count, equalTo(1));
        }
    }

    @Test
    public void testChunkedBatchingOnParam() throws Exception
    {
        UsesBatching b = SqlObjectBuilder.attach(handle, UsesBatching.class);
        List<Something> things = Arrays.asList(new Something(1, "Brian"),
                                               new Something(2, "Henri"),
                                               new Something(3, "Patrick"),
                                               new Something(4, "Robert"),
                                               new Something(5, "Maniax"));
        int[] counts = b.insertChunked(3, things);
        assertThat(counts.length, equalTo(5));
        for (int count : counts) {
            assertThat(count, equalTo(1));
        }
    }

    @Test(expected = UnableToCreateStatementException.class, timeout=5000)
    public void testNoIterable() throws Exception
    {
        BadBatch b = SqlObjectBuilder.attach(handle, BadBatch.class);
        b.insertBeans(new Something(1, "x"));
    }

    @Test(expected = UnableToCreateStatementException.class, timeout=5000)
    public void testNoParameterAtAll() throws Exception
    {
        BadBatch b = SqlObjectBuilder.attach(handle, BadBatch.class);
        b.insertBeans();
    }

    @RegisterMapper(SomethingMapper.class)
    @BatchChunkSize(4)
    @UseStringTemplate3StatementLocator
    public interface UsesBatching
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] insertBeans(@BindBean Iterable<Something> elements);

        @SqlBatch(value = "insert into something (id, name) values (:id, :name)", transactional = false)
        int[] insertBeansNoTx(@BindBean Iterator<Something> elements);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] withConstantValue(@Bind("id") Iterable<Integer> ids, @Bind("name") String name);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] zipArgumentsTogether(@Bind("id") Iterable<Integer> ids, @Bind("name") List<String> name);

        @SqlBatch("insert into something (id, name) values (:it.id, :it.name)")
        @BatchChunkSize(2)
        int[] insertChunked(@BindBean("it") Iterable<Something> its);

        @SqlBatch
        int[] insertChunked(@BatchChunkSize int size, @BindBean("it") Iterable<Something> its);

        @SqlQuery("select count(*) from something")
        int size();
    }

    interface BadBatch
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        int[] insertBeans(@BindBean Something elements); // whoops, no Iterable!

        @SqlBatch("insert into something (id, name) values (0, '')")
        int[] insertBeans(); // whoops, no parameters at all!
    }
}

/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.Bind;
import org.jdbi.v3.sqlobject.BindBean;
import org.jdbi.v3.sqlobject.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.jdbi.v3.util.StringMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestBatching
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
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
                                   .map(StringMapper.FIRST)
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
                                       .map(StringMapper.FIRST)
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

    @BatchChunkSize(4)
    @UseStringTemplate3StatementLocator
    public static interface UsesBatching
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        public int[] insertBeans(@BindBean Iterable<Something> elements);

        @SqlBatch(value = "insert into something (id, name) values (:id, :name)", transactional = false)
        public int[] insertBeansNoTx(@BindBean Iterator<Something> elements);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        public int[] withConstantValue(@Bind("id") Iterable<Integer> ids, @Bind("name") String name);

        @SqlBatch("insert into something (id, name) values (:id, :name)")
        public int[] zipArgumentsTogether(@Bind("id") Iterable<Integer> ids, @Bind("name") List<String> name);

        @SqlBatch("insert into something (id, name) values (:it.id, :it.name)")
        @BatchChunkSize(2)
        public int[] insertChunked(@BindBean("it") Iterable<Something> its);

        @SqlBatch
        public int[] insertChunked(@BatchChunkSize int size, @BindBean("it") Iterable<Something> its);

        @SqlQuery("select count(*) from something")
        public int size();
    }
}

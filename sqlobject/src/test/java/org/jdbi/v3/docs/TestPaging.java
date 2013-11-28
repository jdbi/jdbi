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
package org.jdbi.v3.docs;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

import org.hamcrest.CoreMatchers;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.logging.PrintStreamLog;
import org.jdbi.v3.sqlobject.Bind;
import org.jdbi.v3.sqlobject.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestPaging
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void pagingExample() throws Exception
    {
        Sql sql = SqlObjectBuilder.attach(handle, Sql.class);

        int[] rs = sql.insert(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                              asList("Ami", "Brian", "Cora", "David", "Eric",
                                     "Fernando", "Greta", "Holly", "Inigo", "Joy",
                                     "Keith", "Lisa", "Molly"));

        assertThat(rs, equalTo(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}));

        List<Something> page_one = sql.loadPage(-1, 5);
        assertThat(page_one, CoreMatchers.<List<Something>>equalTo(
                                     ImmutableList.of(new Something(1, "Ami"),
                                                      new Something(2, "Brian"),
                                                      new Something(3, "Cora"),
                                                      new Something(4, "David"),
                                                      new Something(5, "Eric"))));

        List<Something> page_two = sql.loadPage(page_one.get(page_one.size() - 1).getId(), 5);
        assertThat(page_two, CoreMatchers.<List<Something>>equalTo(
                                     ImmutableList.of(new Something(6, "Fernando"),
                                                      new Something(7, "Greta"),
                                                      new Something(8, "Holly"),
                                                      new Something(9, "Inigo"),
                                                      new Something(10, "Joy"))));

    }

    @RegisterMapper(SomethingMapper.class)
    public static interface Sql
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        public int[] insert(@Bind("id") Iterable<Integer> ids, @Bind("name") Iterable<String> names);

        @SqlQuery("select id, name from something where id > :end_of_last_page order by id limit :size")
        public List<Something> loadPage(@Bind("end_of_last_page") int last, @Bind("size") int size);
    }
}

/*
 * Copyright (C) 2004 - 2014 Brian McCallister
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
package org.skife.jdbi.v2.sqlobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.exceptions.UnableToCreateSqlObjectException;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestCreateSqlObjectAnnotation
{
    private DBI dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:" + UUID.randomUUID());
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
    public void testSimpleCreate() throws Exception
    {
        Foo foo = handle.attach(Foo.class);
        foo.insert(1, "Stephane");
        Something s = foo.createBar().findById(1);
        assertThat(s, equalTo(new Something(1, "Stephane")));
    }

    @Test
    public void testInsertAndFind() throws Exception
    {
        Foo foo = handle.attach(Foo.class);
        Something s = foo.insertAndFind(1, "Stephane");
        assertThat(s, equalTo(new Something(1, "Stephane")));
    }

    @Test
    public void testTransactionPropagates() throws Exception
    {
        Foo foo = dbi.onDemand(Foo.class);

        try {
            foo.insertAndFail(1, "Jeff");
            fail("should have raised an exception");
        }
        catch (Exception e){}
        Something n = foo.createBar().findById(1);
        assertThat(n, nullValue());
    }

    public static abstract class Foo
    {
        @CreateSqlObject
        public abstract Bar createBar();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public abstract int insert(@Bind("id") int id, @Bind("name") String name);

        @Transaction
        public Something insertAndFind(int id, String name) {
            insert(id, name);
            return createBar().findById(id);
        }

        @Transaction
        public Something insertAndFail(int id, String name) {
            insert(id, name);
            return createBar().explode();
        }
    }

    public static abstract class Bar
    {
        @SqlQuery("select id, name from something where id = :it")
        public abstract Something findById(@Bind int id);

        public Something explode()
        {
            throw new RuntimeException();
        }

    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testMeaningfulExceptionWhenWrongReturnType() throws Exception {
        expectedException.expect(UnableToCreateSqlObjectException.class);
        expectedException.expectMessage("BogusDao.getNames method is annotated with @SqlUpdate " +
                "so should return void or Number but is returning: java.util.List<java.lang.String>");

        dbi.open(BogusDao.class);
    }

    public static interface BogusDao {
        @SqlUpdate("select name from something")
        public List<String> getNames();
    }
}

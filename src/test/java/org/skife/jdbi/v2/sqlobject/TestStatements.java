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
package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestStatements
{
    private DBI dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
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
    public void testInsert() throws Exception
    {
        Inserter i = SqlObjectBuilder.open(dbi, Inserter.class);

        // this is what is under test here
        int rows_affected = i.insert(2, "Diego");

        String name = handle.createQuery("select name from something where id = 2").mapTo(String.class).first();

        assertEquals(1, rows_affected);
        assertEquals("Diego", name);

        i.close();
    }

    @Test
    public void testInsertWithVoidReturn() throws Exception
    {
        Inserter i = SqlObjectBuilder.open(dbi, Inserter.class);

        // this is what is under test here
        i.insertWithVoidReturn(2, "Diego");

        String name = handle.createQuery("select name from something where id = 2").mapTo(String.class).first();

        assertEquals("Diego", name);

        i.close();
    }

    @Test
    public void testDoubleArgumentBind() throws Exception
    {
        Doubler d = dbi.open(Doubler.class);
        assertTrue(d.doubleTest("wooooot"));
    }

    public static interface Inserter extends CloseMe
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public int insert(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insertWithVoidReturn(@Bind("id") long id, @Bind("name") String name);
    }

    public interface Doubler
    {
        @SqlQuery("select :test = :test")
        boolean doubleTest(@Bind("test") String test);
    }
}

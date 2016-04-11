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
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.customizers.FetchSize;
import org.skife.jdbi.v2.sqlobject.customizers.MaxRows;
import org.skife.jdbi.v2.sqlobject.customizers.QueryTimeOut;
import org.skife.jdbi.v2.sqlobject.customizers.TransactionIsolation;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.skife.jdbi.v2.TransactionIsolationLevel.READ_UNCOMMITTED;

public class TestModifiers
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
    public void testFetchSizeAsArgOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAll(1);
        assertEquals(3, things.size());
    }

    @Test
    public void testFetchSizeOnMethodOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAll();
        assertEquals(3, things.size());
    }

    @Test
    public void testMaxSizeOnMethod() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithMaxRows();
        assertEquals(1, things.size());
    }

    @Test
    public void testMaxSizeOnParam() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithMaxRows(2);
        assertEquals(2, things.size());
    }

    @Test
    public void testQueryTimeOutOnMethodOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithQueryTimeOut();
        assertEquals(3, things.size());
    }

    @Test
    public void testQueryTimeOutOnParamOnlyUsefulWhenSteppingThroughDebuggerSadly() throws Exception
    {
        Spiffy s = SqlObjectBuilder.attach(handle, Spiffy.class);
        s.insert(14, "Tom");
        s.insert(15, "JFA");
        s.insert(16, "Sunny");

        List<Something> things = s.findAllWithQueryTimeOut(2);
        assertEquals(3, things.size());
    }


    @Test
    public void testIsolationLevelOnMethod() throws Exception
    {
        Spiffy spiffy = dbi.open(Spiffy.class);
        IsoLevels iso = dbi.open(IsoLevels.class);

        spiffy.begin();
        spiffy.insert(1, "Tom");

        Something tom = iso.findById(1);
        assertThat(tom, notNullValue());

        spiffy.rollback();

        Something not_tom = iso.findById(1);
        assertThat(not_tom, nullValue());

        spiffy.close();
        iso.close();
    }

    @Test
    public void testIsolationLevelOnParam() throws Exception
    {
        Spiffy spiffy = dbi.open(Spiffy.class);
        IsoLevels iso = dbi.open(IsoLevels.class);

        spiffy.begin();
        spiffy.insert(1, "Tom");

        Something tom = iso.findById(1, READ_UNCOMMITTED);
        assertThat(tom, notNullValue());

        spiffy.rollback();

        Something not_tom = iso.findById(1);
        assertThat(not_tom, nullValue());

        spiffy.close();
        iso.close();
    }

    public static interface Spiffy extends CloseMe, Transactional<Spiffy>
    {
        @SqlQuery("select id, name from something where id = :id")
        public Something byId(@Bind("id") long id);

        @SqlQuery("select id, name from something")
        public List<Something> findAll(@FetchSize(1) int fetchSize);

        @SqlQuery("select id, name from something")
        @FetchSize(2)
        public List<Something> findAll();


        @SqlQuery("select id, name from something")
        public List<Something> findAllWithMaxRows(@MaxRows(1) int fetchSize);

        @SqlQuery("select id, name from something")
        @MaxRows(1)
        public List<Something> findAllWithMaxRows();

        @SqlQuery("select id, name from something")
        public List<Something> findAllWithQueryTimeOut(@QueryTimeOut(1) int timeOutInSeconds);

        @SqlQuery("select id, name from something")
        @QueryTimeOut(1)
        public List<Something> findAllWithQueryTimeOut();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }

    public static interface IsoLevels extends CloseMe
    {
        @TransactionIsolation(READ_UNCOMMITTED)
        @SqlQuery("select id, name from something where id = :id")
        public Something findById(@Bind("id") int id);

        @SqlQuery("select id, name from something where id = :id")
        public Something findById(@Bind("id") int id, @TransactionIsolation TransactionIsolationLevel iso);

    }

}

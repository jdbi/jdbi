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
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.sql.Connection;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestNewApiOnDbiAndHandle
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
    public void testOpenNewSpiffy() throws Exception
    {
        final Connection c;
        try (Spiffy spiffy = SqlObjectBuilder.open(dbi, Spiffy.class)) {
            spiffy.insert(new Something(1, "Tim"));
            spiffy.insert(new Something(2, "Diego"));

            assertEquals("Diego", spiffy.findNameById(2));
            c = spiffy.getHandle().getConnection();
        }
        assertTrue(c.isClosed());
    }

    @Test
    public void testOnDemandSpiffy() throws Exception
    {
        Spiffy spiffy = SqlObjectBuilder.attach(handle, Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertEquals("Diego", spiffy.findNameById(2));
    }

    @Test
    public void testAttach() throws Exception
    {
        Spiffy spiffy = SqlObjectBuilder.attach(handle, Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertEquals("Diego", spiffy.findNameById(2));
    }


    static interface Spiffy extends GetHandle, Closeable
    {
        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something s);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }
}

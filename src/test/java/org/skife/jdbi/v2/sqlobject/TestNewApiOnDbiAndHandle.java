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

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


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

    @Test
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testOpenNewSpiffy() throws Exception
    {
        Spiffy spiffy = dbi.open(Spiffy.class);
        try {
            spiffy.insert(new Something(1, "Tim"));
            spiffy.insert(new Something(2, "Diego"));

            assertEquals("Diego", spiffy.findNameById(2));
        }
        finally {
            dbi.close(spiffy);
        }
        assertTrue(spiffy.getHandle().getConnection().isClosed());
    }

    @Test
    public void testOnDemandSpiffy() throws Exception
    {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertEquals("Diego", spiffy.findNameById(2));
    }

    @Test
    public void testAttach() throws Exception
    {
        Spiffy spiffy = handle.attach(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertEquals("Diego", spiffy.findNameById(2));
    }


    static interface Spiffy extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@Bind(value = "it", binder = SomethingBinderAgainstBind.class) Something s);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

}

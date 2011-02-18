/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionStatus;

public class TestMixinInterfaces extends TestCase
{
    private DBI dbi;
    private Handle handle;


    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }


    public void testGetHandle() throws Exception
    {
        WithGetHandle g = EOD.attach(handle, WithGetHandle.class);
        Handle h = g.getHandle();

        assertSame(handle, h);
    }

    public void testBeginAndCommitTransaction() throws Exception
    {
        TransactionStuff txl = EOD.attach(handle, TransactionStuff.class);

        txl.insert(8, "Mike");

        txl.begin();
        txl.updateName(8, "Miker");
        assertEquals("Miker", txl.byId(8).getName());
        txl.rollback();

        assertEquals("Mike", txl.byId(8).getName());

    }

    public void testInTransaction() throws Exception
    {
        TransactionStuff txl = EOD.attach(handle, TransactionStuff.class);

        Something s = txl.inTransaction(new Transaction<Something, TransactionStuff>() {
            public Something inTransaction(TransactionStuff conn, TransactionStatus status) throws Exception
            {
                return null;
            }
        });
    }

    public static interface WithGetHandle extends CloseMe, GetHandle
    {

    }

    public static interface TransactionStuff extends CloseMe, Transactional<TransactionStuff> {

        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        public Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        public void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@Bind("id") long id, @Bind("name") String name);
    }
}


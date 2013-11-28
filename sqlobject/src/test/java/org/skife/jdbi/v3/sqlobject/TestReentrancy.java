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
package org.skife.jdbi.v3.sqlobject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.skife.jdbi.v3.DBI;
import org.skife.jdbi.v3.Handle;
import org.skife.jdbi.v3.Something;
import org.skife.jdbi.v3.TransactionCallback;
import org.skife.jdbi.v3.TransactionStatus;
import org.skife.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v3.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v3.tweak.HandleCallback;
import org.skife.jdbi.v3.util.StringMapper;

import junit.framework.TestCase;

public class TestReentrancy extends TestCase
{
    private DBI    dbi;
    private Handle handle;

    interface TheBasics extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);
    }

    @Test
    public void testGetHandleProvidesSeperateHandle() throws Exception
    {
        final TheBasics dao = SqlObjectBuilder.onDemand(dbi, TheBasics.class);
        Handle h = dao.getHandle();

        try {
            h.execute("insert into something (id, name) values (1, 'Stephen')");
            fail("should have raised exception, connection will be closed at this point");
        }
        catch (UnableToCreateStatementException e) {
            // happy path
        }
    }

    @Test
    public void testHandleReentrant() throws Exception
    {
        final TheBasics dao = SqlObjectBuilder.onDemand(dbi, TheBasics.class);

        dao.withHandle(new HandleCallback<Void>()
        {
            @Override
            public Void withHandle(Handle handle) throws Exception
            {
                dao.insert(new Something(7, "Martin"));

                handle.createQuery("SELECT 1").list();

                return null;
            }
        });
    }

    @Test
    public void testTxnReentrant() throws Exception
    {
        final TheBasics dao = SqlObjectBuilder.onDemand(dbi, TheBasics.class);

        dao.withHandle(new HandleCallback<Void>()
        {
            @Override
            public Void withHandle(Handle handle) throws Exception
            {
                handle.inTransaction(new TransactionCallback<Void>()
                {
                    @Override
                    public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        dao.insert(new Something(1, "x"));

                        List<String> rs = conn.createQuery("select name from something where id = 1")
                                              .map(StringMapper.FIRST)
                                              .list();
                        assertThat(rs.size(), equalTo(1));

                        conn.createQuery("SELECT 1").list();
                        return null;
                    }
                });

                return null;
            }
        });
    }


    @Override
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));

        dbi = new DBI(ds);

        dbi.registerMapper(new SomethingMapper());

        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @Override
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }
}

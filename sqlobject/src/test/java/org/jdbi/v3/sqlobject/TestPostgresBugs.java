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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.io.IOException;

import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.TransactionIsolationLevel;
import org.jdbi.v3.TransactionStatus;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.mixins.Transactional;
import org.jdbi.v3.tweak.HandleCallback;
import org.jdbi.v3.util.IntegerMapper;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPostgresBugs
{
    private static DBI createDbi()
    {
        String user = System.getenv("POSTGRES_USER");
        String pass = System.getenv("POSTGRES_PASS");
        String url = System.getenv("POSTGRES_URL");

        assumeThat(user, notNullValue());
//        assumeThat(pass, notNullValue());
        assumeThat(url, notNullValue());

        org.postgresql.Driver.getVersion();
        return new DBI(url, user, pass);
    }

    @BeforeClass
    public static void setUp() throws Exception
    {
        createDbi().withHandle(new HandleCallback<Object>()
        {
            @Override
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("create table if not exists something (id int primary key, name varchar(100))");
                handle.execute("delete from something");
                return null;
            }
        });
    }

    @Test
    public void testConnected() throws Exception
    {
        DBI dbi = createDbi();
        int four = dbi.withHandle(new HandleCallback<Integer>()
        {
            @Override
            public Integer withHandle(Handle handle) throws Exception
            {
                return handle.createQuery("select 2 + 2").map(IntegerMapper.FIRST).first();
            }
        });

        assertThat(four, equalTo(4));
    }

    @Test
    public void testTransactions() throws Exception
    {
        DBI dbi = createDbi();
        Dao dao = SqlObjectBuilder.onDemand(dbi, Dao.class);

        dao.begin();
        Something s = dao.insertAndFetch(1, "Brian");
        dao.commit();
        assertThat(s, equalTo(new Something(1, "Brian")));
    }

    @Test
    public void testExplicitBeginAndInTransaction() throws Exception
    {
        DBI dbi = createDbi();
        Dao dao = SqlObjectBuilder.onDemand(dbi, Dao.class);

        dao.begin();
        Something s = dao.inTransaction(new org.jdbi.v3.Transaction<Something, Dao>() {

            @Override
            public Something inTransaction(Dao transactional, TransactionStatus status) throws Exception
            {
                return  transactional.insertAndFetch(1, "Brian");
            }
        });

        dao.commit();
        assertThat(s, equalTo(new Something(1, "Brian")));
    }


    @RegisterMapper(SomethingMapper.class)
    public static abstract class Dao implements Transactional<Dao>
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public abstract void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        public abstract Something findById(@Bind("id") int id);

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        public Something insertAndFetch(int id, String name)
        {
            insert(id, name);
            return findById(id);
        }

        @Transaction
        public Something insertAndFetchWithNestedTransaction(int id, String name)
        {
            return insertAndFetch(id, name);
        }

        @Transaction
        public Something failed(int id, String name) throws IOException
        {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}

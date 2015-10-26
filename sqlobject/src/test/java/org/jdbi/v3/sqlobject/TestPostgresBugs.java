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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.jdbi.v3.PGDatabaseRule;
import org.jdbi.v3.Something;
import org.jdbi.v3.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.mixins.Transactional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPostgresBugs
{
    @Rule
    public PGDatabaseRule db = new PGDatabaseRule();

    @Before
    public void setUp() throws Exception
    {
        db.getDbi().useHandle(handle -> {
            handle.execute("create table if not exists something (id int primary key, name varchar(100))");
            handle.execute("delete from something");
        });
    }

    @Test
    public void testConnected() throws Exception
    {
        int four = db.getDbi().withHandle(handle ->
                handle.createQuery("select 2 + 2").mapTo(Integer.class).findOnly());

        assertThat(four, equalTo(4));
    }

    @Test
    public void testTransactions() throws Exception
    {
        Dao dao = SqlObjectBuilder.onDemand(db.getDbi(), Dao.class);

        Something s = dao.insertAndFetch(1, "Brian");
        assertThat(s, equalTo(new Something(1, "Brian")));
    }

    @Test
    public void testExplicitTransaction() throws Exception
    {
        Dao dao = SqlObjectBuilder.onDemand(db.getDbi(), Dao.class);

        Something s = dao.inTransaction((transactional, status) -> {
            transactional.insert(1, "Brian");
            return transactional.findById(1);
        });

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
        public Something failed(int id, String name) throws IOException
        {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}

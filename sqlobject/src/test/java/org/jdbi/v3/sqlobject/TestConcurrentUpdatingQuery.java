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

import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestConcurrentUpdatingQuery
{
    private DBI    dbi;
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
    public void testConcurrentUpdateableResultSet() throws Exception
    {
        handle.execute("insert into something (id, name) values (7, 'Tim')");
        handle.createQuery("select id, name from something where id = :id")
                .bind("id", 7)
                .concurrentUpdatable()
                .map((index, r, ctx) -> {
                    r.updateString("name", "Tom");
                    r.updateRow();
                    return null;
                }).list();

        final String name = handle.createQuery("select name from something where id = :id")
                .bind("id", 7)
                .mapTo(String.class)
                .findOnly();

        assertEquals("Tom", name);
    }

}

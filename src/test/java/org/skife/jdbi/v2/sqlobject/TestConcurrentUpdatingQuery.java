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
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.Assert.assertEquals;


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
                .map(new ResultSetMapper<Object>() {
                    @Override
                    public Object map(final int index,
                                      final ResultSet r,
                                      final StatementContext ctx) throws SQLException {
                        r.updateString("name", "Tom");
                        r.updateRow();
                        return null;
                    }
                }).list();

        final String name = handle.createQuery("select name from something where id = :id")
                .bind("id", 7)
                .mapTo(String.class)
                .first();

        assertEquals("Tom", name);
    }

}

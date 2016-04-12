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
import static org.junit.Assert.fail;

import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.DBI;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestReentrancy
{
    private DBI    dbi;
    private Handle handle;

    public interface TheBasics extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);
    }

    @Test
    public void testGetHandleProvidesSeperateHandle() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);
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
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(handle1 -> {
            dao.insert(new Something(7, "Martin"));

            handle1.createQuery("SELECT 1").list();

            return null;
        });
    }

    @Test
    public void testTxnReentrant() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(handle1 -> {
            handle1.useTransaction((conn, status) -> {
                dao.insert(new Something(1, "x"));

                List<String> rs = conn.createQuery("select name from something where id = 1")
                        .mapTo(String.class)
                        .list();
                assertThat(rs.size(), equalTo(1));

                conn.createQuery("SELECT 1").list();
            });

            return null;
        });
    }


    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));

        dbi = DBI.create(ds);
        dbi.installPlugin(new SqlObjectPlugin());
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
}

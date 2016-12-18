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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestReentrancy
{
    private Jdbi    dbi;
    private Handle handle;

    public interface TheBasics extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testGetHandleProvidesSeperateHandle() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);
        Handle h = dao.getHandle();

        h.execute("insert into something (id, name) values (1, 'Stephen')");
    }

    @Test
    public void testHandleReentrant() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(handle1 -> {
            dao.insert(new Something(7, "Martin"));

            handle1.createQuery("SELECT 1").mapToMap().list();

            return null;
        });
    }

    @Test
    public void testTxnReentrant() throws Exception
    {
        final TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.withHandle(handle1 -> {
            handle1.useTransaction(h -> {
                dao.insert(new Something(1, "x"));

                List<String> rs = h.createQuery("select name from something where id = 1")
                        .mapTo(String.class)
                        .list();
                assertThat(rs).hasSize(1);

                h.createQuery("SELECT 1").mapTo(int.class).list();
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

        dbi = Jdbi.create(ds);
        dbi.installPlugin(new SqlObjectPlugin());
        dbi.registerRowMapper(new SomethingMapper());

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

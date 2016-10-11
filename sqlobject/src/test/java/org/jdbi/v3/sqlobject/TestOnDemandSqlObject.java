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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.ResultIterator;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.exception.JdbiException;
import org.jdbi.v3.core.exception.TransactionException;
import org.jdbi.v3.core.exception.UnableToCloseResourceException;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.sqlobject.customizers.UseRowMapper;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.mixins.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOnDemandSqlObject
{
    private Jdbi    dbi;
    private Handle handle;
    private final HandleTracker tracker = new HandleTracker();
    private JdbcDataSource ds;

    @Before
    public void setUp() throws Exception
    {
        ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));
        dbi = Jdbi.create(ds);
        dbi.installPlugin(new SqlObjectPlugin());
        handle = dbi.open();
        handle.execute("create table something (id int primary key, name varchar(100))");

        dbi.installPlugin(tracker);
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void testAPIWorks() throws Exception
    {
        Spiffy s = dbi.onDemand(Spiffy.class);

        s.insert(7, "Bill");

        String bill = dbi.open().createQuery("select name from something where id = 7").mapTo(String.class).findOnly();

        assertThat(bill).isEqualTo("Bill");
    }

    @Test(expected=TransactionException.class)
    public void testExceptionOnClose() throws Exception {
        JdbiPlugin plugin = new JdbiPlugin() {
            @Override
            public Handle customizeHandle(Handle handle) {
                Handle h = spy(handle);
                when(h.createUpdate(anyString())).thenThrow(new TransactionException("connection reset"));
                doThrow(new UnableToCloseResourceException("already closed", null)).when(h).close();
                return h;
            }
        };
        dbi.installPlugin(plugin);

        Spiffy s = dbi.onDemand(Spiffy.class);
        s.insert(1, "Tom");
    }

    @Test
    public void testIteratorCloseHandleOnError() throws Exception {
        Spiffy s = dbi.onDemand(Spiffy.class);
        assertThatExceptionOfType(JdbiException.class).isThrownBy(s::crashNow);

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorClosedOnReadError() throws Exception {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);
        spiffy.insert(1, "Tom");

        Iterator<Something> i = spiffy.crashOnFirstRead();
        assertThatExceptionOfType(JdbiException.class).isThrownBy(i::next);

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorClosedIfEmpty() throws Exception {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);

        spiffy.findAll();

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorPrepatureClose() throws Exception {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);
        spiffy.insert(1, "Tom");

        try (ResultIterator<Something> all = spiffy.findAll()) {}

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testSqlFromExternalFileWorks() throws Exception
    {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);
        ExternalSql external = dbi.onDemand(ExternalSql.class);

        spiffy.insert(1, "Tom");
        spiffy.insert(2, "Sam");

        List<Something> all = external.findAll();
        assertThat(all).hasSize(2);
    }

    public interface Spiffy extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);

        @SqlQuery("select name, id from something")
        @UseRowMapper(SomethingMapper.class)
        ResultIterator<Something> findAll();

        @SqlQuery("select * from crash now")
        @UseRowMapper(SomethingMapper.class)
        Iterator<Something> crashNow();

        @SqlQuery("select name, id from something")
        @UseRowMapper(CrashingMapper.class)
        Iterator<Something> crashOnFirstRead();

    }

    public interface TransactionStuff extends GetHandle, Transactional<TransactionStuff>
    {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }

    @UseClasspathSqlLocator
    public interface ExternalSql extends GetHandle
    {
        @SqlQuery("all-something")
        @UseRowMapper(SomethingMapper.class)
        List<Something> findAll();
    }

    static class CrashingMapper implements RowMapper<Something>
    {
        @Override
        public Something map(ResultSet r, StatementContext ctx) throws SQLException
        {
            throw new SQLException("protocol error");
        }
    }

    static class HandleTracker implements JdbiPlugin
    {
        final List<Handle> openedHandle = new ArrayList<>();

        @Override
        public Handle customizeHandle(Handle handle) {
            openedHandle.add(handle);
            return handle;
        }

        boolean hasOpenedHandle() throws SQLException {
            for (Handle h : openedHandle) {
                if (!h.getConnection().isClosed()) return true;
            }
            return false;
        }
    }
}

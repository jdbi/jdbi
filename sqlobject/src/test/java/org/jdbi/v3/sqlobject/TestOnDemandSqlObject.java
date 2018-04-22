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
import static org.mockito.ArgumentMatchers.anyString;
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
import org.jdbi.v3.core.CloseException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.result.ResultIterator;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestOnDemandSqlObject {
    private Jdbi db;
    private Handle handle;
    private final HandleTracker tracker = new HandleTracker();
    private JdbcDataSource ds;

    @Before
    public void setUp() throws Exception {
        ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));
        db = Jdbi.create(ds);
        db.installPlugin(new SqlObjectPlugin());
        handle = db.open();
        handle.execute("create table something (id int primary key, name varchar(100))");

        db.installPlugin(tracker);
    }

    @After
    public void tearDown() throws Exception {
        handle.close();
    }

    @Test
    public void testAPIWorks() throws Exception {
        Spiffy s = db.onDemand(Spiffy.class);

        s.insert(7, "Bill");

        String bill = db.open().createQuery("select name from something where id = 7").mapTo(String.class).findOnly();

        assertThat(bill).isEqualTo("Bill");
    }

    @Test(expected=TransactionException.class)
    public void testExceptionOnClose() throws Exception {
        JdbiPlugin plugin = new JdbiPlugin() {
            @Override
            public Handle customizeHandle(Handle handle) {
                Handle h = spy(handle);
                when(h.createUpdate(anyString())).thenThrow(new TransactionException("connection reset"));
                doThrow(new CloseException("already closed", null)).when(h).close();
                return h;
            }
        };
        db.installPlugin(plugin);

        Spiffy s = db.onDemand(Spiffy.class);
        s.insert(1, "Tom");
    }

    @Test
    public void testIteratorCloseHandleOnError() throws Exception {
        Spiffy s = db.onDemand(Spiffy.class);
        assertThatExceptionOfType(JdbiException.class).isThrownBy(s::crashNow);

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorClosedOnReadError() throws Exception {
        Spiffy spiffy = db.onDemand(Spiffy.class);
        spiffy.insert(1, "Tom");

        Iterator<Something> i = spiffy.crashOnFirstRead();
        assertThatExceptionOfType(JdbiException.class).isThrownBy(i::next);

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorClosedIfEmpty() throws Exception {
        Spiffy spiffy = db.onDemand(Spiffy.class);

        spiffy.findAll();

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorPrepatureClose() throws Exception {
        Spiffy spiffy = db.onDemand(Spiffy.class);
        spiffy.insert(1, "Tom");

        try (ResultIterator<Something> all = spiffy.findAll()) {}

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testSqlFromExternalFileWorks() throws Exception {
        Spiffy spiffy = db.onDemand(Spiffy.class);
        ExternalSql external = db.onDemand(ExternalSql.class);

        spiffy.insert(1, "Tom");
        spiffy.insert(2, "Sam");

        List<Something> all = external.findAll();
        assertThat(all).hasSize(2);
    }

    public interface Spiffy {
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

    public interface TransactionStuff extends Transactional<TransactionStuff> {
        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }

    @UseClasspathSqlLocator
    public interface ExternalSql {
        @SqlQuery("all-something")
        @UseRowMapper(SomethingMapper.class)
        List<Something> findAll();
    }

    public static class CrashingMapper implements RowMapper<Something> {
        @Override
        public Something map(ResultSet r, StatementContext ctx) throws SQLException {
            throw new SQLException("fake protocol error");
        }
    }

    static class HandleTracker implements JdbiPlugin {
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

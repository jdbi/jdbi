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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestOnDemandSqlObject {

    private final HandleTracker tracker = new HandleTracker();

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Jdbi jdbi;

    @BeforeEach
    public void setUp() throws Exception {
        jdbi = h2Extension.getJdbi();
        // do not pull into the extension clause, otherwise the shared handle counts!
        jdbi.installPlugin(tracker);
        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testAPIWorks() {
        Spiffy s = jdbi.onDemand(Spiffy.class);

        s.insert(7, "Bill");

        try (Handle handle = jdbi.open()) {
            String bill = handle.createQuery("select name from something where id = 7").mapTo(String.class).one();

            assertThat(bill).isEqualTo("Bill");
        }
    }

    @Test
    public void testExceptionOnClose() {
        // mockito is a terrible piece of software and should not be used.
        // this is needed because of https://github.com/mockito/mockito/issues/2331
        h2Extension.enableLeakChecker(false);

        JdbiPlugin plugin = new JdbiPlugin() {
            @Override
            public Handle customizeHandle(Handle handle) {
                Handle spyHandle = spy(handle);
                when(spyHandle.createUpdate(anyString())).thenThrow(new TransactionException("connection reset"));
                doCallRealMethod().doThrow(new CloseException("already closed", null)).when(spyHandle).close();
                return spyHandle;
            }
        };
        jdbi.installPlugin(plugin);

        Spiffy s = jdbi.onDemand(Spiffy.class);
        assertThatThrownBy(() -> s.insert(1, "Tom")).isInstanceOf(TransactionException.class);
    }

    @Test
    public void testIteratorCloseHandleOnError() throws Exception {
        Spiffy s = jdbi.onDemand(Spiffy.class);
        assertThatExceptionOfType(JdbiException.class).isThrownBy(s::crashNow);

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorClosedOnReadError() throws Exception {
        Spiffy spiffy = jdbi.onDemand(Spiffy.class);
        spiffy.insert(1, "Tom");

        Iterator<Something> i = spiffy.crashOnFirstRead();
        assertThatExceptionOfType(JdbiException.class).isThrownBy(i::next);

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorClosedIfEmpty() throws Exception {
        Spiffy spiffy = jdbi.onDemand(Spiffy.class);

        spiffy.findAll();

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testIteratorPrepatureClose() throws Exception {
        Spiffy spiffy = jdbi.onDemand(Spiffy.class);
        spiffy.insert(1, "Tom");

        try (ResultIterator<Something> all = spiffy.findAll()) {
            // skip
        }

        assertThat(tracker.hasOpenedHandle()).isFalse();
    }

    @Test
    public void testSqlFromExternalFileWorks() {
        Spiffy spiffy = jdbi.onDemand(Spiffy.class);
        ExternalSql external = jdbi.onDemand(ExternalSql.class);

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
                if (!h.getConnection().isClosed()) {
                    return true;
                }
            }
            return false;
        }
    }
}

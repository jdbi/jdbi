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

import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.ConnectionException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestNewApiOnDbiAndHandle {
    private Jdbi db;
    private Handle handle;

    @Before
    public void setUp() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        db = Jdbi.create(ds);
        db.installPlugin(new SqlObjectPlugin());
        db.registerRowMapper(new SomethingMapper());
        handle = db.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testOpenNewSpiffy() throws Exception {
        final AtomicReference<Connection> c = new AtomicReference<>();

        db.useExtension(Spiffy.class, spiffy -> {
            spiffy.insert(new Something(1, "Tim"));
            spiffy.insert(new Something(2, "Diego"));

            assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
            c.set(spiffy.getHandle().getConnection());
        });

        assertThat(c.get().isClosed()).isTrue();
    }

    @Test
    public void testOnDemandSpiffy() {
        Spiffy spiffy = db.onDemand(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
    }

    @Test
    public void testAttach() {
        Spiffy spiffy = handle.attach(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
    }

    @Test
    public void testCorrectExceptionIfUnableToConnectOnDemand() {
        assertThatThrownBy(() -> Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
            .installPlugin(new SqlObjectPlugin())
            .onDemand(Spiffy.class)
            .findNameById(1)).isInstanceOf(ConnectionException.class);
    }

    @Test
    public void testCorrectExceptionIfUnableToConnectOnOpen() {
        assertThatThrownBy(() -> Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
            .installPlugin(new SqlObjectPlugin())
            .open()
            .attach(Spiffy.class)).isInstanceOf(ConnectionException.class);
    }

    @Test
    public void testCorrectExceptionIfUnableToConnectOnAttach() {
        assertThatThrownBy(() -> Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
            .installPlugin(new SqlObjectPlugin())
            .open()
            .attach(Spiffy.class)).isInstanceOf(ConnectionException.class);
    }

    public interface Spiffy extends SqlObject {
        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@BindSomething("it") Something s);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }
}

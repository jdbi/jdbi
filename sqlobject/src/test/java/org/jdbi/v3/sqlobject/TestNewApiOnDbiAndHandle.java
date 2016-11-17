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

import java.sql.Connection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMappers;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.exception.UnableToObtainConnectionException;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class TestNewApiOnDbiAndHandle
{
    private Jdbi    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
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

    @Test
    public void testOpenNewSpiffy() throws Exception
    {
        final AtomicReference<Connection> c = new AtomicReference<>();

        dbi.useExtension(Spiffy.class, spiffy -> {
            spiffy.insert(new Something(1, "Tim"));
            spiffy.insert(new Something(2, "Diego"));

            assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
            c.set(spiffy.getHandle().getConnection());
        });

        assertThat(c.get().isClosed()).isTrue();
    }

    @Test
    public void testOnDemandSpiffy() throws Exception
    {
        Spiffy spiffy = dbi.onDemand(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
    }

    @Test
    public void testAttach() throws Exception
    {
        Spiffy spiffy = handle.attach(Spiffy.class);

        spiffy.insert(new Something(1, "Tim"));
        spiffy.insert(new Something(2, "Diego"));

        assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
    }

    @Test(expected = UnableToObtainConnectionException.class)
    public void testCorrectExceptionIfUnableToConnectOnDemand(){
        Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
                .installPlugin(new SqlObjectPlugin())
                .onDemand(Spiffy.class)
                .findNameById(1);
    }

    @Test(expected = UnableToObtainConnectionException.class)
    public void testCorrectExceptionIfUnableToConnectOnOpen(){
        Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
                .installPlugin(new SqlObjectPlugin())
                .open()
                .attach(Spiffy.class);
    }

    @Test(expected = UnableToObtainConnectionException.class)
    public void testCorrectExceptionIfUnableToConnectOnAttach(){
        Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
                .installPlugin(new SqlObjectPlugin())
                .open()
                .attach(Spiffy.class);
    }

    public interface Spiffy extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@BindSomething("it") Something s);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }
}

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
package org.jdbi.sqlobject;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicReference;

import org.jdbi.core.ConnectionException;
import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestNewApiOnDbiAndHandle {
    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2().withInitializer(TestingInitializers.something()).withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private Jdbi jdbi;

    @BeforeEach
    public void setUp() {
        jdbi = h2Extension.getJdbi();
        handle = h2Extension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void testOpenNewSpiffy() throws Exception {
        final AtomicReference<Connection> c = new AtomicReference<>();

        jdbi.useExtension(Spiffy.class, spiffy -> {
            spiffy.insert(new Something(1, "Tim"));
            spiffy.insert(new Something(2, "Diego"));

            assertThat(spiffy.findNameById(2)).isEqualTo("Diego");
            c.set(spiffy.getHandle().getConnection());
        });

        assertThat(c.get().isClosed()).isTrue();
    }

    @Test
    public void testOnDemandSpiffy() {
        Spiffy spiffy = jdbi.onDemand(Spiffy.class);

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
        assertThatThrownBy(() -> {
            try (Handle handle =
                Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
                    .installPlugin(new SqlObjectPlugin())
                    .open()) {
                handle.attach(Spiffy.class);
            }
        }).isInstanceOf(ConnectionException.class);
    }

    @Test
    public void testCorrectExceptionIfUnableToConnectOnAttach() {
        assertThatThrownBy(() -> {
            try (Handle handle =
                Jdbi.create("jdbc:mysql://invalid.invalid/test", "john", "scott")
                    .installPlugin(new SqlObjectPlugin())
                    .open()) {
                handle.attach(Spiffy.class);
            }
        }).isInstanceOf(ConnectionException.class);
    }

    public interface Spiffy extends SqlObject {
        @SqlUpdate("insert into something (id, name) values (:it.id, :it.name)")
        void insert(@BindSomething("it") Something s);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }
}

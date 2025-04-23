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

import java.lang.reflect.Method;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestMixinInterfaces {

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
    public void testGetHandle() {
        WithGetHandle g = handle.attach(WithGetHandle.class);
        Handle h = g.getHandle();

        assertThat(h).isSameAs(handle);
    }

    @Test
    public void testWithHandle() {
        WithGetHandle g = handle.attach(WithGetHandle.class);
        String name = g.withHandle(h -> {
            h.execute("insert into something (id, name) values (8, 'Mike')");

            return h.createQuery("select name from something where id = 8").mapTo(String.class).one();
        });

        assertThat(name).isEqualTo("Mike");
    }

    @Test
    public void testWithHandleExtensionMethod() throws Exception {
        Method withHandleMethod = SqlObject.class.getMethod("withHandle", HandleCallback.class);

        WithGetHandle g = handle.attach(WithGetHandle.class);
        String name = g.withHandle(h -> {
            ExtensionMethod extensionMethod = handle.getExtensionMethod();
            // ensure that the current extension method on the handle is the method that is currently executing
            assertThat(extensionMethod.getMethod()).isEqualTo(withHandleMethod);
            return "Mike";
        });

        assertThat(name).isEqualTo("Mike");
    }

    @Test
    public void testUseHandle() {
        WithGetHandle g = handle.attach(WithGetHandle.class);
        g.useHandle(h -> {
            h.execute("insert into something(id, name) values (9, 'James')");

            assertThat(h.createQuery("select name from something where id = 9")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("James");
        });
    }

    @Test
    public void testOverrideUseHandleFails() throws Exception {
        Method getHandleMethod = ExplicitUseHandle.class.getMethod("useHandle", HandleConsumer.class);

        assertThatThrownBy(() -> {
            ExplicitUseHandle g = handle.attach(ExplicitUseHandle.class);
            g.useHandle(h -> {
                assertThat(h).isNotNull();
                assertThat(h.getExtensionMethod().getMethod()).isEqualTo(getHandleMethod);
            });
        }).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Method ExplicitUseHandle.useHandle has no registered extension handler!");
    }

    @Test
    public void testUseHandleExtensionMethod() throws Exception {
        Method withHandleMethod = SqlObject.class.getMethod("withHandle", HandleCallback.class);

        WithGetHandle g = handle.attach(WithGetHandle.class);
        g.useHandle(h -> {
            // useHandle is delegated to withHandle
            ExtensionMethod extensionMethod = handle.getExtensionMethod();
            assertThat(extensionMethod.getMethod()).isEqualTo(withHandleMethod);
        });
    }

    @Test
    public void testBeginAndCommitTransaction() {
        TransactionStuff txl = handle.attach(TransactionStuff.class);

        txl.insert(8, "Mike");

        txl.begin();
        txl.updateName(8, "Miker");
        assertThat(txl.byId(8).getName()).isEqualTo("Miker");
        txl.rollback();

        assertThat(txl.byId(8).getName()).isEqualTo("Mike");
    }

    @Test
    public void testInTransaction() {
        TransactionStuff txl = handle.attach(TransactionStuff.class);
        txl.insert(7, "Keith");

        Something s = txl.inTransaction(h -> h.byId(7));

        assertThat(s.getName()).isEqualTo("Keith");
    }

    @Test
    public void testInTransactionWithLevel() {
        TransactionStuff txl = handle.attach(TransactionStuff.class);
        txl.insert(7, "Keith");

        Something s = txl.inTransaction(TransactionIsolationLevel.SERIALIZABLE, conn -> {
            assertThat(conn.getHandle().getTransactionIsolationLevel())
                    .isEqualTo(TransactionIsolationLevel.SERIALIZABLE);
            return conn.byId(7);
        });

        assertThat(s.getName()).isEqualTo("Keith");
    }

    @Test
    public void testTransactionIsolationActuallyHappens() {
        TransactionStuff txl = handle.attach(TransactionStuff.class);
        jdbi.useExtension(TransactionStuff.class, tx2 -> {
            txl.insert(8, "Mike");

            txl.begin();

            txl.updateName(8, "Miker");
            assertThat(txl.byId(8).getName()).isEqualTo("Miker");
            assertThat(tx2.byId(8).getName()).isEqualTo("Mike");

            txl.commit();

            assertThat(tx2.byId(8).getName()).isEqualTo("Miker");
        });
    }

    @Test
    public void testJustJdbiTransactions() {
        try (Handle h1 = jdbi.open();
                Handle h2 = jdbi.open()) {
            h1.execute("insert into something (id, name) values (8, 'Mike')");

            h1.begin();
            h1.execute("update something set name = 'Miker' where id = 8");

            assertThat(h2.createQuery("select name from something where id = 8")
                    .mapTo(String.class)
                    .one())
                    .isEqualTo("Mike");
            h1.commit();
        }
    }

    private interface WithGetHandle extends SqlObject {}

    private interface ExplicitUseHandle extends SqlObject {
        @Override
        void useHandle(HandleConsumer consumer);
    }

    private interface TransactionStuff extends Transactional<TransactionStuff> {

        @SqlQuery("select id, name from something where id = :id")
        @UseRowMapper(SomethingMapper.class)
        Something byId(@Bind("id") long id);

        @SqlUpdate("update something set name = :name where id = :id")
        void updateName(@Bind("id") long id, @Bind("name") String name);

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") long id, @Bind("name") String name);
    }
}


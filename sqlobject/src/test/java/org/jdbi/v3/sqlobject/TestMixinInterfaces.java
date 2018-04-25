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

import java.util.UUID;
import org.h2.jdbcx.JdbcDataSource;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transactional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestMixinInterfaces {
    private Jdbi db;
    private Handle handle;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(String.format("jdbc:h2:mem:%s;MVCC=TRUE", UUID.randomUUID()));
        db = Jdbi.create(ds);
        db.installPlugin(new SqlObjectPlugin());
        handle = db.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testGetHandle() throws Exception {
        WithGetHandle g = handle.attach(WithGetHandle.class);
        Handle h = g.getHandle();

        assertThat(h).isSameAs(handle);
    }

    @Test
    public void testWithHandle() throws Exception {
        WithGetHandle g = handle.attach(WithGetHandle.class);
        String name = g.withHandle(handle1 -> {
            handle1.execute("insert into something (id, name) values (8, 'Mike')");

            return handle1.createQuery("select name from something where id = 8").mapTo(String.class).findOnly();
        });

        assertThat(name).isEqualTo("Mike");
    }

    @Test
    public void testUseHandle() throws Exception {
        WithGetHandle g = handle.attach(WithGetHandle.class);
        g.useHandle(h -> h.execute("insert into something(id, name) values (9, 'James')"));

        assertThat(handle.createQuery("select name from something where id = 9")
                .mapTo(String.class)
                .findOnly())
                .isEqualTo("James");
    }

    @Test
    public void testBeginAndCommitTransaction() throws Exception {
        TransactionStuff txl = handle.attach(TransactionStuff.class);

        txl.insert(8, "Mike");

        txl.begin();
        txl.updateName(8, "Miker");
        assertThat(txl.byId(8).getName()).isEqualTo("Miker");
        txl.rollback();

        assertThat(txl.byId(8).getName()).isEqualTo("Mike");
    }

    @Test
    public void testInTransaction() throws Exception {
        TransactionStuff txl = handle.attach(TransactionStuff.class);
        txl.insert(7, "Keith");

        Something s = txl.inTransaction(h -> h.byId(7));

        assertThat(s.getName()).isEqualTo("Keith");
    }

    @Test
    public void testInTransactionWithLevel() throws Exception {
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
    public void testTransactionIsolationActuallyHappens() throws Exception {
        TransactionStuff txl = handle.attach(TransactionStuff.class);
        db.useExtension(TransactionStuff.class, tx2 -> {
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
    public void testJustJdbiTransactions() throws Exception {
        try (Handle h1 = db.open();
             Handle h2 = db.open()) {
            h1.execute("insert into something (id, name) values (8, 'Mike')");

            h1.begin();
            h1.execute("update something set name = 'Miker' where id = 8");

            assertThat(h2.createQuery("select name from something where id = 8")
                    .mapTo(String.class)
                    .findOnly())
                    .isEqualTo("Mike");
            h1.commit();
        }
    }

    private interface WithGetHandle extends SqlObject {}

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


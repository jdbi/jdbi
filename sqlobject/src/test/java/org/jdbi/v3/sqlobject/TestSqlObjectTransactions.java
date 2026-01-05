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

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.transaction.AbstractDelegatingTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.sqlobject.transaction.Transactional;
import org.jdbi.v3.sqlobject.transaction.TransactionalConsumer;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TestSqlObjectTransactions {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withInitializer(TestingInitializers.users());

    private Jdbi jdbi;
    private CountingTransactionHandler transactionHandler;

    @BeforeEach
    public void setUp() {
        this.jdbi = h2Extension.getJdbi();
        this.jdbi.registerRowMapper(User.class, ConstructorMapper.of(User.class));
        this.transactionHandler = new CountingTransactionHandler(jdbi.getTransactionHandler());
        jdbi.setTransactionHandler(transactionHandler);
    }

    /**
     * Use an annotated method to create objects in the database. Use a handle attached sql object.
     * <br>
     * Each method is called in its own transaction. Therefore two transactions are created and committed.
     */
    @Test
    public void createInHandleUsesTwoTransactions() {
        jdbi.useHandle(handle -> {
            TransactionDao dao = handle.attach(TransactionDao.class);

            dao.createUser(1, "Alice");
            dao.createUser(2, "Bob");
        });

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(2, 0);
    }

    /**
     * Use an annotated method to create objects in the database. Use a handle attached sql object. The handle is inside a transaction
     * when the sql object is attached.
     * <br>
     * Each method is called in the parent handle transaction. Therefore only one transactions are created and committed.
     */
    @Test
    public void createInTransactionHandleUsesOneTransaction() {
        jdbi.useHandle(handle ->
                handle.useTransaction(transactionHandle -> {
                    TransactionDao dao = transactionHandle.attach(TransactionDao.class);

                    dao.createUser(1, "Alice");
                    dao.createUser(2, "Bob");
                }));

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(1, 0);
    }

    /**
     * Use an annotated method to create objects in the database. Attach as an on-demand object to the Jdbi.
     * <br>
     * Each method is called in its own transaction. Therefore two transactions are created and committed.
     */
    @Test
    public void createInJdbiOnDemandUsesTwoTransactions() {

        TransactionDao dao = jdbi.onDemand(TransactionDao.class);
        dao.createUser(1, "Alice");
        dao.createUser(2, "Bob");

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(2, 0);
    }

    /**
     * Use an annotated method to create objects in the database. Attach as an on-demand object to the Jdbi. Use the
     * {@link Transactional#useTransaction(TransactionalConsumer)} callback which creates a transaction boundary.
     * <br>
     * Each method is called within the transaction boundary. Therefore only one transactions are created and committed.
     */
    @Test
    public void createInJdbiOnDemandTransactionalUsesOneTransaction() {

        TransactionDao dao = jdbi.onDemand(TransactionDao.class);

        dao.useTransaction(transactionDao -> {
            transactionDao.createUser(1, "Alice");
            transactionDao.createUser(2, "Bob");
        });

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(1, 0);
    }

    /**
     * Use an annotated method to create objects in the database. Use the {@link Jdbi#withExtension} callback to manage the Sql object.
     * <br>
     * Each method is called in its own transaction. Therefore two transactions are created and committed.
     */
    @Test
    public void createInJdbiExtensionUsesTwoTransactions() {

        jdbi.useExtension(TransactionDao.class, dao -> {
            dao.createUser(1, "Alice");
            dao.createUser(2, "Bob");
        });

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(2, 0);
    }

    /**
     * Use an annotated method to create objects in the database. Use the {@link Jdbi#withExtension} callback to manage the Sql object. Use the
     * {@link Transactional#useTransaction(TransactionalConsumer)} callback which creates a transaction boundary.
     * <br>
     * Each method is called within the transaction boundary. Therefore only one transactions are created and committed.
     */
    @Test
    public void createInJdbiExtensionTransactionalUsesOneTransaction() {

        jdbi.useExtension(TransactionDao.class, dao ->
                dao.useTransaction(transactionDao -> {
                    transactionDao.createUser(1, "Alice");
                    transactionDao.createUser(2, "Bob");
                }));

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(1, 0);
    }

    /**
     * Use an annotated method to create objects in the database. Use the {@link Jdbi#withExtension} callback to manage the Sql object. Use the
     * {@link Transactional#useTransaction(TransactionalConsumer)} callback which creates a transaction boundary.
     * <br>
     * Each method is called within the transaction boundary. Therefore only one transactions are created and committed.
     */
    @Test
    public void nestedTransactionalUsesOneTransaction() {

        jdbi.useExtension(TransactionDao.class, dao ->
                dao.useTransaction(transactionDao1 -> {
                    transactionDao1.createUser(1, "Alice");

                    transactionDao1.useTransaction(transactionDao2 ->
                            transactionDao2.createUser(2, "Bob"));
                }));

        List<User> users = jdbi.withHandle(handle -> handle.createQuery("SELECT * from users").mapTo(User.class).list());
        assertThat(users.size()).isEqualTo(2);

        transactionHandler.assertTransactionResult(1, 0);
    }

    public interface TransactionDao extends Transactional<TransactionDao> {

        @Transaction
        @SqlUpdate("INSERT INTO users VALUES (:id, :name)")
        void createUser(int id, String name);
    }

    public static class User {

        private final int id;
        private final String name;

        public User(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class CountingTransactionHandler extends AbstractDelegatingTransactionHandler {

        private final AtomicInteger beginTransactions = new AtomicInteger();
        private final AtomicInteger commitTransactions = new AtomicInteger();
        private final AtomicInteger rollbackTransactions = new AtomicInteger();

        CountingTransactionHandler(TransactionHandler delegate) {
            super(delegate);
        }

        @Override
        public void begin(Handle handle) {
            super.begin(handle);
            beginTransactions.incrementAndGet();
        }

        @Override
        public void commit(Handle handle) {
            super.commit(handle);
            commitTransactions.incrementAndGet();
        }

        @Override
        public void rollback(Handle handle) {
            super.rollback(handle);
            rollbackTransactions.incrementAndGet();
        }

        public void assertTransactionResult(int commit, int rollback) {
            assertThat(beginTransactions.get()).isEqualTo(commitTransactions.get() + rollbackTransactions.get());
            assertThat(commitTransactions.get()).isEqualTo(commit);
            assertThat(rollbackTransactions.get()).isEqualTo(rollback);
        }
    }
}

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
package jdbi.doc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.exception.TransactionException;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.core.transaction.TransactionCallback;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.TransactionStatus;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.jdbi.v3.sqlobject.Transaction;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jdbi.doc.ResultsTest.User;

public class TransactionTest {

    @ClassRule
    public static PostgresDbRule db = new PostgresDbRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Handle handle;
    private Jdbi jdbi;

    @Before
    public void getHandle() {
        jdbi = db.getJdbi();
        handle = db.getSharedHandle();
        handle.registerRowMapper(ConstructorMapper.of(User.class));
    }

    @Before
    public void setUp() throws Exception {
        handle.useTransaction((th, status) -> {
            th.execute("DROP TABLE IF EXISTS users");
            th.execute("CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR)");
            for (String name : Arrays.asList("Alice", "Bob", "Charlie", "Data")) {
                th.execute("INSERT INTO users(name) VALUES (?)", name);
            }
        });
    }

    @Test
    public void inTransaction() {
        User u = findUserById(2).orElseThrow(() -> new AssertionError("No user found"));
        assertThat(u.id).isEqualTo(2);
        assertThat(u.name).isEqualTo("Bob");
    }

    // tag::simpleTransaction[]
    public Optional<User> findUserById(long id) {
        return handle.inTransaction((h, status) ->
                h.createQuery("SELECT * FROM users WHERE id=:id")
                        .bind("id", id)
                        .mapTo(User.class)
                        .findFirst());
    }
    // end::simpleTransaction[]

    // tag::sqlObjectTransaction[]
    @Test
    public void sqlObjectTransaction() {
        assertThat(handle.attach(UserDao.class).findUserById(3).map(u -> u.name)).contains("Charlie");
    }

    public interface UserDao {
        @SqlQuery("SELECT * FROM users WHERE id=:id")
        @Transaction
        Optional<User> findUserById(int id);
    }
    // end::sqlObjectTransaction[]

    @Test
    public void sqlObjectTransactionIsolation() {
        UserDao2 dao = handle.attach(UserDao2.class);
        dao.insertUser("Echo");
        assertThat(handle.attach(UserDao.class).findUserById(5).map(u -> u.name)).contains("Echo");
    }

    public interface UserDao2 extends UserDao {
        // tag::sqlObjectTransactionIsolation[]
        @SqlUpdate("INSERT INTO USERS (name) VALUES (:name)")
        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        void insertUser(String name);
        // end::sqlObjectTransactionIsolation[]
    }

    @Test
    public void sqlObjectNestedTransactions() {
        NestedTransactionDao dao = handle.attach(NestedTransactionDao.class);
        dao.outerMethodCallsInnerWithSameLevel();
        dao.outerMethodWithLevelCallsInnerMethodWithNoLevel();

        exception.expect(TransactionException.class);
        dao.outerMethodWithOneLevelCallsInnerMethodWithAnotherLevel();
    }

    public interface NestedTransactionDao extends GetHandle {
        // tag::sqlObjectNestedTransaction[]
        @Transaction(TransactionIsolationLevel.READ_UNCOMMITTED)
        default void outerMethodCallsInnerWithSameLevel() {
            // this works: isolation levels agree
            innerMethodSameLevel();
        }

        @Transaction(TransactionIsolationLevel.READ_UNCOMMITTED)
        default void innerMethodSameLevel() {}

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        default void outerMethodWithLevelCallsInnerMethodWithNoLevel() {
            // this also works: inner method doesn't specify a level, so the outer method controls.
            innerMethodWithNoLevel();
        }

        @Transaction
        default void innerMethodWithNoLevel() {}

        @Transaction(TransactionIsolationLevel.REPEATABLE_READ)
        default void outerMethodWithOneLevelCallsInnerMethodWithAnotherLevel() throws TransactionException {
            // error! inner method specifies a different isolation level.
            innerMethodWithADifferentLevel();
        }

        @Transaction(TransactionIsolationLevel.SERIALIZABLE)
        default void innerMethodWithADifferentLevel() {}
        // end::sqlObjectNestedTransaction[]
    }

    // tag::serializable[]
    public interface IntListDao {
        @SqlUpdate("CREATE TABLE ints (value INTEGER)")
        void create();

        @SqlQuery("SELECT sum(value) FROM ints")
        int sum();

        @SqlUpdate("INSERT INTO ints(value) VALUES(:value)")
        void insert(int value);
    }

    static class SumAndInsert implements Callable<Integer>, TransactionCallback<Integer, Exception> {
        private final Jdbi jdbi;
        private final CountDownLatch latch;

        public SumAndInsert(CountDownLatch latch, Jdbi jdbi) {
            this.latch = latch;
            this.jdbi = jdbi;
        }

        @Override
        public Integer inTransaction(Handle handle, TransactionStatus status) throws Exception {
            IntListDao dao = handle.attach(IntListDao.class);
            int sum = dao.sum();

            // First time through, make sure neither transaction writes until both have read
            latch.countDown();
            latch.await();

            // Now do the write.
            dao.insert(sum);
            return sum;
        }

        @Override
        public Integer call() throws Exception {
            // Get a connection and run the transaction
            return jdbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, this);
        }
    }

    @Test
    public void serializableTransaction() throws Exception {
        // Automatically rerun transactions
        jdbi.setTransactionHandler(new SerializableTransactionRunner());

        // Set up some values
        IntListDao dao = handle.attach(IntListDao.class);
        dao.create();
        dao.insert(10);
        dao.insert(20);

        ExecutorService executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(2);

        // Both of these would calculate 10 + 20 = 30, but that violates serialization!
        SumAndInsert txn1 = new SumAndInsert(latch, jdbi);
        SumAndInsert txn2 = new SumAndInsert(latch, jdbi);

        Future<Integer> result1 = executor.submit(txn1);
        Future<Integer> result2 = executor.submit(txn2);

        // One of them gets 30, the other gets 10 + 20 + 30 = 60
        // This assertion fails under any isolation level below SERIALIZABLE!
        assertThat(result1.get() + result2.get()).isEqualTo(30 + 60);

        executor.shutdown();
    }
    // end::serializable[]
}

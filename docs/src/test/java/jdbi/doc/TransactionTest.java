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
import java.util.function.BiConsumer;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.postgres.PostgresDbRule;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.testing.JdbiRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jdbi.doc.ResultsTest.User;

public class TransactionTest {

    @ClassRule
    public static JdbiRule dbRule = PostgresDbRule.rule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Handle handle;
    private Jdbi db;

    @Before
    public void getHandle() {
        db = dbRule.getJdbi();
        handle = dbRule.getHandle();
        handle.registerRowMapper(ConstructorMapper.factory(User.class));
    }

    @Before
    public void setUp() throws Exception {
        handle.useTransaction(h -> {
            h.execute("DROP TABLE IF EXISTS users");
            h.execute("CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR)");
            for (String name : Arrays.asList("Alice", "Bob", "Charlie", "Data")) {
                h.execute("INSERT INTO users(name) VALUES (?)", name);
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
        return handle.inTransaction(h ->
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

    public interface NestedTransactionDao extends SqlObject {
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

    @Test
    public void serializableTransaction() throws Exception {
        // tag::serializable[]
        // Automatically rerun transactions
        db.setTransactionHandler(new SerializableTransactionRunner());

        // Set up some values
        BiConsumer<Handle, Integer> insert = (h, i) -> h.execute("INSERT INTO ints(value) VALUES(?)", i);
        handle.execute("CREATE TABLE ints (value INTEGER)");
        insert.accept(handle, 10);
        insert.accept(handle, 20);

        // Run the following twice in parallel, and synchronize
        ExecutorService executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(2);

        Callable<Integer> sumAndInsert = () ->
            db.inTransaction(TransactionIsolationLevel.SERIALIZABLE, h -> {
                // Both read initial state of table
                int sum = h.select("SELECT sum(value) FROM ints").mapTo(int.class).findOnly();

                // First time through, make sure neither transaction writes until both have read
                latch.countDown();
                latch.await();

                // Now do the write.
                insert.accept(h, sum);
                return sum;
            });

        // Both of these would calculate 10 + 20 = 30, but that violates serialization!
        Future<Integer> result1 = executor.submit(sumAndInsert);
        Future<Integer> result2 = executor.submit(sumAndInsert);

        // One of the transactions gets 30, the other will abort and automatically rerun.
        // On the second attempt it will compute 10 + 20 + 30 = 60, seeing the update from its sibling.
        // This assertion fails under any isolation level below SERIALIZABLE!
        assertThat(result1.get() + result2.get()).isEqualTo(30 + 60);

        executor.shutdown();
        // end::serializable[]
    }
}

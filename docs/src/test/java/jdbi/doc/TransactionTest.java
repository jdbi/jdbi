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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.softwareforge.testing.postgres.junit5.EmbeddedPgExtension;
import de.softwareforge.testing.postgres.junit5.MultiDatabaseBuilder;
import jdbi.doc.ResultsTest.User;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.transaction.SerializableTransactionRunner;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("HiddenField")
public class TransactionTest {

    @RegisterExtension
    public static EmbeddedPgExtension pg = MultiDatabaseBuilder.instanceWithDefaults().build();

    @RegisterExtension
    public JdbiExtension pgExtension = JdbiExtension.postgres(pg)
            .withPlugin(new PostgresPlugin())
            .withPlugin(new SqlObjectPlugin());

    private Handle handle;
    private Jdbi jdbi;

    @BeforeEach
    public void setUp() {
        jdbi = pgExtension.getJdbi();
        jdbi.registerRowMapper(ConstructorMapper.factory(User.class));
        handle = pgExtension.openHandle();

        handle.useTransaction(h -> {
            h.execute("DROP TABLE IF EXISTS users");
            h.execute("CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR)");
            for (String name : Arrays.asList("Alice", "Bob", "Charlie", "Data")) {
                h.execute("INSERT INTO users(name) VALUES (?)", name);
            }
        });
    }

    @AfterEach
    public void tearDown() {
        handle.close();
    }

    @Test
    public void inHandleTransaction() {
        User u = findUserById(handle, 2).orElseThrow(() -> new AssertionError("No user found"));
        assertThat(u.id).isEqualTo(2);
        assertThat(u.name).isEqualTo("Bob");
    }

    @Test
    public void inJdbiTransaction() {
        User u = findUserById(jdbi, 2).orElseThrow(() -> new AssertionError("No user found"));
        assertThat(u.id).isEqualTo(2);
        assertThat(u.name).isEqualTo("Bob");
    }

    // tag::simpleTransaction[]
    // use a Jdbi object to create transaction
    public Optional<User> findUserById(Jdbi jdbi, long id) {
        return jdbi.inTransaction(transactionHandle ->
                transactionHandle.createQuery("SELECT * FROM users WHERE id=:id")
                        .bind("id", id)
                        .mapTo(User.class)
                        .findFirst());
    }

    // use a Handle object to create transaction
    public Optional<User> findUserById(Handle handle, long id) {
        return handle.inTransaction(transactionHandle ->
                transactionHandle.createQuery("SELECT * FROM users WHERE id=:id")
                        .bind("id", id)
                        .mapTo(User.class)
                        .findFirst());
    }
    // end::simpleTransaction[]

    @Test
    public void sqlObjectTransaction() {
        assertThat(handle.attach(UserDao.class).findUserById(3).map(u -> u.name)).contains("Charlie");
    }

    public interface UserDao {

        // tag::sqlObjectTransaction[]
        @Transaction
        @SqlQuery("SELECT * FROM users WHERE id=:id")
        Optional<User> findUserById(int id);
        // end::sqlObjectTransaction[]
    }

    @Test
    public void sqlObjectTransactionIsolation() {
        UserDao2 dao = handle.attach(UserDao2.class);
        dao.insertUser("Echo");
        assertThat(handle.attach(UserDao.class).findUserById(5).map(u -> u.name)).contains("Echo");
    }

    public interface UserDao2 extends UserDao {

        // tag::sqlObjectTransactionIsolation[]
        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        @SqlUpdate("INSERT INTO USERS (name) VALUES (:name)")
        void insertUser(String name);
        // end::sqlObjectTransactionIsolation[]
    }

    @Test
    public void sqlObjectNestedTransactions() {
        NestedTransactionDao dao = handle.attach(NestedTransactionDao.class);
        dao.outerMethodCallsInnerWithSameLevel();
        dao.outerMethodWithLevelCallsInnerMethodWithNoLevel();

        assertThatThrownBy(dao::outerMethodWithOneLevelCallsInnerMethodWithAnotherLevel)
                .isInstanceOf(TransactionException.class);
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
        jdbi.setTransactionHandler(new SerializableTransactionRunner());
        handle.execute("CREATE TABLE ints (value INTEGER)");

        // Set up some values
        handle.execute("INSERT INTO ints (value) VALUES(?)", 10);
        handle.execute("INSERT INTO ints (value) VALUES(?)", 20);

        // Run the following twice in parallel, and synchronize
        ExecutorService executor = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(2);

        Callable<Integer> sumAndInsert = () ->
                jdbi.inTransaction(TransactionIsolationLevel.SERIALIZABLE, transactionHandle -> {
                    // Both threads read initial state of table
                    int sum = transactionHandle.select("SELECT sum(value) FROM ints").mapTo(int.class).one();

                    // synchronize threads, make sure that they each has successfully read the data
                    latch.countDown();
                    latch.await();

                    // Now do the write.
                    synchronized (this) {
                        // handle can be used by multiple threads, but not at the same time
                        transactionHandle.execute("INSERT INTO ints (value) VALUES(?)", sum);
                    }
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

        List<Integer> results = handle.createQuery("SELECT * from ints").mapTo(Integer.class).list();
        // both threads have committed their result
        assertThat(results.size()).isEqualTo(4);

    }
}

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
package org.jdbi.core.async;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdbi.core.Handle;
import org.jdbi.core.HandleCallback;
import org.jdbi.core.HandleConsumer;
import org.jdbi.core.Jdbi;
import org.jdbi.core.extension.ExtensionFactory;
import org.jdbi.core.extension.HandleSupplier;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.transaction.TransactionIsolationLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.core.extension.ExtensionFactory.FactoryFlag.NON_VIRTUAL_FACTORY;
import static org.junit.jupiter.api.Assertions.fail;

public class JdbiExecutorTest {

    private static final HandleCallback<Integer, Exception> RUN_QUERY =
        handle -> handle.createQuery("SELECT COUNT(*) FROM users").mapTo(Integer.class).one();

    private static final HandleConsumer<Exception> RUN_UPDATE =
        handle -> handle.execute("INSERT INTO users VALUES (3, 'Jim')");

    static class TestExtensionFactory implements ExtensionFactory {

        @Override
        public boolean accepts(Class<?> extensionType) {
            return TestExtension.class.equals(extensionType);
        }

        @Override
        public Set<FactoryFlag> getFactoryFlags() {
            return EnumSet.of(NON_VIRTUAL_FACTORY);
        }

        @Override
        public <E> E attach(Class<E> extensionType, HandleSupplier handleSupplier) {
            return extensionType.cast(new TestExtensionImpl(handleSupplier));
        }
    }

    public interface TestExtension {

        Handle getHandle();

        default int get() {
            return 5;
        }

        default void set() {}
    }

    public static class TestExtensionImpl implements TestExtension {

        private final HandleSupplier handleSupplier;

        TestExtensionImpl(HandleSupplier handleSupplier) {
            this.handleSupplier = handleSupplier;
        }

        @Override
        public Handle getHandle() {
            return handleSupplier.getHandle();
        }
    }

    @RegisterExtension
    private final H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private JdbiExecutor jdbiExecutor = null;
    private Jdbi jdbi;

    @BeforeEach
    void setup() {
        h2Extension.getJdbi().useHandle(H2DatabaseExtension.USERS_INITIALIZER::initialize);
        jdbi = h2Extension.getJdbi().registerExtension(new TestExtensionFactory());
        jdbiExecutor = JdbiExecutor.create(jdbi, Executors.newFixedThreadPool(2));
    }

    @Test
    void testWithHandle() {
        assertThat(
            jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
    }

    @Test
    void testWithHandleThrowsException() {
        assertThat(
            jdbiExecutor.withHandle(
                handle -> {
                    throw new RuntimeException();
                }))
            .failsWithin(Duration.ofSeconds(10))
            .withThrowableOfType(ExecutionException.class)
            .withCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void testUseHandle() {
        assertThat(
            jdbiExecutor.useHandle(RUN_UPDATE))
            .succeedsWithin(Duration.ofSeconds(10));
        assertThat(jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
    }

    @Test
    void testInTransaction() {
        assertThat(
            jdbiExecutor.inTransaction(checkInTxn(RUN_QUERY)))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
    }

    @Test
    void testInTransactionWithLevel() {
        assertThat(
            jdbiExecutor.inTransaction(TransactionIsolationLevel.READ_COMMITTED, checkReadCommitted(RUN_QUERY)))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
    }

    @Test
    void testUseTransaction() {
        assertThat(
            jdbiExecutor.useTransaction(checkInTxn(RUN_UPDATE)))
            .succeedsWithin(Duration.ofSeconds(10));
        assertThat(jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
    }

    @Test
    void testUseTransactionWithLevel() {
        assertThat(
            jdbiExecutor.useTransaction(TransactionIsolationLevel.READ_COMMITTED, checkReadCommitted(RUN_UPDATE)))
            .succeedsWithin(Duration.ofSeconds(10));
        assertThat(jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
    }

    @Test
    void testWithExtension() {
        assertThat(
            jdbiExecutor.withExtension(TestExtension.class, TestExtension::get)
        ).succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(5);
    }

    @Test
    void testUseExtension() {
        assertThat(
            jdbiExecutor.useExtension(TestExtension.class, TestExtension::set)
        ).succeedsWithin(Duration.ofSeconds(10));
    }

    @Test
    void testParallelExecutionIsSeparate() {
        CountDownLatch before = new CountDownLatch(1);
        CountDownLatch after = new CountDownLatch(2);

        CompletionStage<Integer> stage1 = jdbiExecutor.inTransaction(TransactionIsolationLevel.REPEATABLE_READ, handle -> {
            // modify in thread 1
            RUN_UPDATE.useHandle(handle);
            // notify update is done
            before.countDown();
            assertThat(handle.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.REPEATABLE_READ);
            int count = RUN_QUERY.withHandle(handle);
            after.countDown();
            // wait for both before continuing
            if (!after.await(10, TimeUnit.SECONDS)) {
                fail("after.await timed out");
            }
            return count;
        });

        CompletionStage<Integer> stage2 = jdbiExecutor.inTransaction(TransactionIsolationLevel.READ_COMMITTED, handle -> {
            // check in thread 2
            // wait for update done
            if (!before.await(10, TimeUnit.SECONDS)) {
                fail("before.await timed out");
            }
            assertThat(handle.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.READ_COMMITTED);
            int count = RUN_QUERY.withHandle(handle);
            after.countDown();
            // wait for both before continuing
            if (!after.await(10, TimeUnit.SECONDS)) {
                fail("after.await timed out");
            }
            return count;
        });

        assertThat(stage1)
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
        assertThat(stage2)
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
    }

    <R, X extends Exception> HandleCallback<R, X> checkInTxn(HandleCallback<R, X> callback) {
        return h -> {
            assertThat(h.isInTransaction()).isTrue();
            return callback.withHandle(h);
        };
    }

    <X extends Exception> HandleConsumer<X> checkInTxn(HandleConsumer<X> consumer) {
        return h -> {
            assertThat(h.isInTransaction()).isTrue();
            consumer.useHandle(h);
        };
    }

    <R, X extends Exception> HandleCallback<R, X> checkReadCommitted(HandleCallback<R, X> callback) {
        return h -> {
            assertThat(h.isInTransaction()).isTrue();
            assertThat(h.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.READ_COMMITTED);
            return callback.withHandle(h);
        };
    }

    <X extends Exception> HandleConsumer<X> checkReadCommitted(HandleConsumer<X> consumer) {
        return h -> {
            assertThat(h.isInTransaction()).isTrue();
            assertThat(h.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.READ_COMMITTED);
            consumer.useHandle(h);
        };
    }
}

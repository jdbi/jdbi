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
package org.jdbi.v3.core.async;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.HandleConsumer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionFactory;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.extension.ExtensionFactory.FactoryFlag.NON_VIRTUAL_FACTORY;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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

    private ExecutorService executor;
    private JdbiExecutor jdbiExecutor = null;
    private Jdbi jdbi;

    @BeforeEach
    void setup() {
        h2Extension.getJdbi().useHandle(H2DatabaseExtension.USERS_INITIALIZER::initialize);
        jdbi = spy(h2Extension.getJdbi().registerExtension(new TestExtensionFactory()));
        executor = Executors.newFixedThreadPool(5);
        jdbiExecutor = JdbiExecutor.create(jdbi, executor);
    }

    @AfterEach
    void tearDown() throws Exception {
        executor.shutdown();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).withFailMessage("Could not shut down executor").isTrue();
    }

    @Test
    void testWithHandle() {
        assertThat(
            jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
        verify(jdbi).withHandle(any());
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
        verify(jdbi).withHandle(any());
    }

    @Test
    void testUseHandle() {
        assertThat(
            jdbiExecutor.useHandle(RUN_UPDATE))
            .succeedsWithin(Duration.ofSeconds(10));
        assertThat(jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
        verify(jdbi).useHandle(any());
    }

    @Test
    void testInTransaction() {
        assertThat(
            jdbiExecutor.inTransaction(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
        verify(jdbi).inTransaction(any());
    }

    @Test
    void testInTransactionWithLevel() {
        assertThat(
            jdbiExecutor.inTransaction(TransactionIsolationLevel.READ_COMMITTED, RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
        verify(jdbi).inTransaction(eq(TransactionIsolationLevel.READ_COMMITTED), any());
    }

    @Test
    void testUseTransaction() {
        assertThat(
            jdbiExecutor.useTransaction(RUN_UPDATE))
            .succeedsWithin(Duration.ofSeconds(10));
        assertThat(jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
        verify(jdbi).useTransaction(any());
    }

    @Test
    void testUseTransactionWithLevel() {
        assertThat(
            jdbiExecutor.useTransaction(TransactionIsolationLevel.READ_COMMITTED, RUN_UPDATE))
            .succeedsWithin(Duration.ofSeconds(10));
        assertThat(jdbiExecutor.withHandle(RUN_QUERY))
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);
        verify(jdbi).useTransaction(eq(TransactionIsolationLevel.READ_COMMITTED), any());
    }

    @Test
    void testWithExtension() {
        assertThat(
            jdbiExecutor.withExtension(TestExtension.class, TestExtension::get)
        ).succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(5);
        verify(jdbi).withExtension(eq(TestExtension.class), any());
    }

    @Test
    void testUseExtension() {
        assertThat(
            jdbiExecutor.useExtension(TestExtension.class, TestExtension::set)
        ).succeedsWithin(Duration.ofSeconds(10));
        verify(jdbi).useExtension(eq(TestExtension.class), any());
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
            .withFailMessage("stage 1 did not complete successfully.")
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(3);

        assertThat(stage2)
            .withFailMessage("stage 2 did not complete successfully.")
            .succeedsWithin(Duration.ofSeconds(10))
            .isEqualTo(2);
    }
}

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
package org.jdbi.v3.core.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A {@link DelegatingTransactionHandler} subclass that overrides only
 * {@link TransactionHandler#inTransaction(Handle, HandleCallback)} must see every transaction,
 * whether or not the caller requested an isolation level.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/2900">issue 2900</a>
 */
public class TestIssue2900 {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private CountingTransactionHandler countingHandler;

    @BeforeEach
    public void setUp() {
        countingHandler = new CountingTransactionHandler(LocalTransactionHandler.binding());
        h2Extension.getJdbi().setTransactionHandler(countingHandler);
    }

    @Test
    public void bothArityPathsUseTheOverride() {
        try (Handle handle = h2Extension.openHandle()) {
            handle.inTransaction(h -> "no level");
            assertThat(countingHandler.count()).isEqualTo(1);

            handle.inTransaction(TransactionIsolationLevel.READ_COMMITTED, h -> "with level");
            assertThat(countingHandler.count()).isEqualTo(2);

            handle.inTransaction(TransactionIsolationLevel.UNKNOWN, h -> "unknown level");
            assertThat(countingHandler.count()).isEqualTo(3);
        }
    }

    @Test
    public void defaultAppliesAndRestoresLevel() {
        try (Handle handle = h2Extension.openHandle()) {
            TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();

            // call the handler directly: unlike a call through Handle.inTransaction, this pins
            // the default method's own save and restore of the isolation level
            countingHandler.inTransaction(handle, TransactionIsolationLevel.SERIALIZABLE, h -> {
                assertThat(h.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.SERIALIZABLE);
                return null;
            });

            assertThat(handle.getTransactionIsolationLevel()).isEqualTo(initial);
            assertThat(countingHandler.count()).isEqualTo(1);
        }
    }

    @Test
    public void levelRestoredWhenCallbackThrows() {
        try (Handle handle = h2Extension.openHandle()) {
            TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
            IllegalStateException boom = new IllegalStateException("boom");

            assertThatThrownBy(() ->
                    countingHandler.inTransaction(handle, TransactionIsolationLevel.SERIALIZABLE, h -> {
                        throw boom;
                    }))
                    .isSameAs(boom);

            assertThat(handle.getTransactionIsolationLevel()).isEqualTo(initial);
            assertThat(countingHandler.count()).isEqualTo(1);
        }
    }

    @Test
    public void restoreFailureSuppressedWhenCallbackThrows() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        AtomicInteger level = new AtomicInteger(Connection.TRANSACTION_READ_COMMITTED);
        Mockito.when(connection.getTransactionIsolation()).thenAnswer(invocation -> level.get());
        Mockito.doAnswer(invocation -> {
            int target = invocation.getArgument(0);
            if (target == Connection.TRANSACTION_READ_COMMITTED) {
                throw new SQLException("restore fails");
            }
            level.set(target);
            return null;
        }).when(connection).setTransactionIsolation(Mockito.anyInt());

        TransactionHandler handler = new DelegatingTransactionHandler(new NoOpTransactionHandler());
        IllegalStateException boom = new IllegalStateException("boom");

        try (Handle handle = Jdbi.create(() -> connection).open()) {
            assertThatThrownBy(() ->
                    handler.inTransaction(handle, TransactionIsolationLevel.SERIALIZABLE, h -> {
                        throw boom;
                    }))
                    .isSameAs(boom)
                    .satisfies(e -> assertThat(e.getSuppressed())
                            .singleElement()
                            .isInstanceOf(UnableToManipulateTransactionIsolationLevelException.class));
        }
    }

    /**
     * Handle.inTransaction(level, callback) saves and restores the level itself, so this case
     * calls the handler directly to pin the behavior of the default method on its own.
     */
    @Test
    public void directHandlerCallWithUnknownLevelDoesNotTouchTheLevel() {
        try (Handle handle = h2Extension.openHandle()) {
            handle.setTransactionIsolationLevel(TransactionIsolationLevel.READ_COMMITTED);

            countingHandler.inTransaction(handle, TransactionIsolationLevel.UNKNOWN, h -> {
                h.setTransactionIsolationLevel(TransactionIsolationLevel.SERIALIZABLE);
                return null;
            });

            // UNKNOWN routes straight through, so the default does not restore a level it never set
            assertThat(handle.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.SERIALIZABLE);
            assertThat(countingHandler.count()).isEqualTo(1);
        }
    }

    static class CountingTransactionHandler extends DelegatingTransactionHandler {
        private final AtomicInteger inTransactionCount = new AtomicInteger();

        CountingTransactionHandler(TransactionHandler delegate) {
            super(delegate);
        }

        @Override
        public <R, X extends Exception> R inTransaction(Handle handle, HandleCallback<R, X> callback) throws X {
            inTransactionCount.incrementAndGet();
            return super.inTransaction(handle, callback);
        }

        int count() {
            return inTransactionCount.get();
        }
    }
}

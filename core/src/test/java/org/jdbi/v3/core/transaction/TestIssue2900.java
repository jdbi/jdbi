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

import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

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
    public void isolationLevelAppliedAndRestored() {
        try (Handle handle = h2Extension.openHandle()) {
            TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();

            handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, h ->
                    assertThat(h.getTransactionIsolationLevel()).isEqualTo(TransactionIsolationLevel.SERIALIZABLE));

            assertThat(handle.getTransactionIsolationLevel()).isEqualTo(initial);
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

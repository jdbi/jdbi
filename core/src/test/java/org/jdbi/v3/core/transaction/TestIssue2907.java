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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

public class TestIssue2907 {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void testCustomHandler() {
        var transactionHandler = new CustomTransactionHandler(LocalTransactionHandler.binding());
        h2Extension.getJdbi().setTransactionHandler(transactionHandler);

        try (Handle handle = h2Extension.openHandle()) {
            // Custom code gets called if two args version is used
            handle.inTransaction(h -> "result1");
            assertThat(transactionHandler.getCustomCallCount()).isEqualTo(1);

            // Custom code gets called if three args version is used
            handle.inTransaction(TransactionIsolationLevel.READ_COMMITTED, h -> "result2");

            assertThat(transactionHandler.getCustomCallCount())
                .as("Both 2-arg and 3-arg inTransaction calls should go through the custom 2-arg override")
                .isEqualTo(2);
        }
    }

    /**
     * Ensure that two args method calls custom logic.
     */
    @Test
    public void testTwoArgInTransactionCallsCustomLogic() {
        var customHandler = new CustomTransactionHandler(LocalTransactionHandler.binding());
        h2Extension.getJdbi().setTransactionHandler(customHandler);

        try (Handle handle = h2Extension.openHandle()) {
            // Direct call to 2-arg version works as expected
            handle.inTransaction(h -> "result");
            assertThat(customHandler.getCustomCallCount()).isEqualTo(1);
        }
    }

    /**
     * Ensure that three args method calls custom logic.
     */
    @Test
    public void testThreeArgInTransactionCallsCustomLogic() {
        var customHandler = new CustomTransactionHandler(LocalTransactionHandler.binding());
        h2Extension.getJdbi().setTransactionHandler(customHandler);

        try (Handle handle = h2Extension.openHandle()) {
            // Direct call to 3-arg version works as expected
            handle.inTransaction(TransactionIsolationLevel.READ_COMMITTED, h -> "result");
            assertThat(customHandler.getCustomCallCount()).isEqualTo(1);
        }
    }

    /**
     * A custom transaction handler that only overrides the 2-arg inTransaction method, expecting all transaction calls to go through it for custom
     * housekeeping.
     */
    private static class CustomTransactionHandler extends AbstractDelegatingTransactionHandler {
        private final AtomicInteger customCallCount = new AtomicInteger(0);

        private CustomTransactionHandler(TransactionHandler delegate) {
            super(delegate);
        }

        @Override
        public <R, X extends Exception> R inTransaction(Handle handle,
            HandleCallback<R, X> callback) throws X {
            // Custom housekeeping code that users expect to run on every transaction
            customCallCount.incrementAndGet();
            return super.inTransaction(handle, callback);
        }

        // NOTE: We intentionally DO NOT override the 3-arg version, expecting that
        // it will delegate to the 2-arg version as it does in LocalTransactionHandler.
        // This is the bug - when @Transaction annotation is used, it calls the 3-arg version
        // which directly delegates to the wrapped handler, bypassing our custom logic.

        public int getCustomCallCount() {
            return customCallCount.get();
        }
    }
}

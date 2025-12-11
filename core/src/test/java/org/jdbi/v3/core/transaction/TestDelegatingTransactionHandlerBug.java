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

/**
 * Test that demonstrates the bug where custom DelegatingTransactionHandler subclasses that only override the 2-arg inTransaction method are bypassed when the
 * 3-arg inTransaction method is called (e.g., by the @Transaction annotation).
 *
 * <p><b>BUG DESCRIPTION:</b></p>
 * <p>
 * When users create a custom transaction handler by extending DelegatingTransactionHandler, they often override only the 2-arg
 * {@code inTransaction(Handle, HandleCallback)} method, expecting all transaction logic to flow through it for housekeeping, logging, etc.
 * </p>
 * <p>
 * However, the {@code @Transaction} annotation (and Handle.inTransaction with isolation level) calls the 3-arg
 * {@code inTransaction(Handle, TransactionIsolationLevel, HandleCallback)} version. The base DelegatingTransactionHandler directly delegates this to the
 * wrapped handler, bypassing the custom 2-arg override entirely.
 * </p>
 * <p>
 * This creates a subtle bug where methods annotated with {@code @Transaction} don't go through the custom transaction handling logic, even though the developer
 * expected all transactions to be intercepted.
 * </p>
 * <p>
 * <b>EXPECTED BEHAVIOR:</b> The 3-arg version should delegate to the 2-arg version
 * (after setting isolation level), similar to how LocalTransactionHandler.BoundLocalTransactionHandler implements it.
 * </p>
 * <p>
 * <b>WORKAROUND:</b> Override both methods in subclasses, with the 3-arg version
 * delegating to the 2-arg version after setting the isolation level.
 * </p>
 */
public class TestDelegatingTransactionHandlerBug {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    /**
     * Test that demonstrates the bug: when calling inTransaction with a TransactionIsolationLevel (as @Transaction annotation does), the custom 2-arg
     * inTransaction override is NOT called.
     *
     * <p><b>THIS TEST IS EXPECTED TO FAIL</b> - it demonstrates the bug described in the issue.</p>
     * <p>
     * When this test fails, it proves that the 3-arg inTransaction method bypasses the custom 2-arg override. The test will pass once the bug is fixed by
     * making DelegatingTransactionHandler's 3-arg version delegate to the 2-arg version (after setting the isolation level).
     * </p>
     */
    @Test
    public void testThreeArgInTransactionBypassesCustomTwoArgOverride() {
        var transactionHandler = new CustomTransactionHandler(LocalTransactionHandler.binding());
        h2Extension.getJdbi().setTransactionHandler(transactionHandler);

        try (Handle handle = h2Extension.openHandle()) {
            // When calling the 2-arg version directly, custom logic is invoked
            handle.inTransaction(h -> "result1");
            assertThat(transactionHandler.getCustomCallCount()).isEqualTo(1);

            // When calling the 3-arg version (as @Transaction annotation does),
            // custom logic is BYPASSED because DelegatingTransactionHandler's 3-arg version
            // directly delegates to the wrapped handler instead of calling the 2-arg version
            handle.inTransaction(TransactionIsolationLevel.READ_COMMITTED, h -> "result2");

            // BUG: This assertion will fail because the 2-arg method was not called
            // Expected: 2 (both calls should go through the custom handler)
            // Actual: 1 (only the first call went through the custom handler)
            assertThat(transactionHandler.getCustomCallCount())
                .as("Both 2-arg and 3-arg inTransaction calls should go through the custom 2-arg override")
                .isEqualTo(2);
        }
    }

    /**
     * Test that demonstrates the expected behavior: if the 3-arg version properly delegated to the 2-arg version (like LocalTransactionHandler does), both
     * calls would go through the custom logic.
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
     * Test showing that if a user overrides BOTH methods, they can work around the issue, but this is not obvious and should not be necessary.
     */
    @Test
    public void testWorkaroundByOverridingBothMethods() {
        var transactionHandler = new WorkaroundTransactionHandler(LocalTransactionHandler.binding());
        h2Extension.getJdbi().setTransactionHandler(transactionHandler);

        try (Handle handle = h2Extension.openHandle()) {
            // When calling the 2-arg version directly, custom logic is invoked
            handle.inTransaction(h -> "result1");
            assertThat(transactionHandler.getCustomCallCount()).isEqualTo(1);

            // When calling the 3-arg version (as @Transaction annotation does),
            // custom logic is BYPASSED because DelegatingTransactionHandler's 3-arg version
            // directly delegates to the wrapped handler instead of calling the 2-arg version
            handle.inTransaction(TransactionIsolationLevel.READ_COMMITTED, h -> "result2");

            // BUG: This assertion will fail because the 2-arg method was not called
            // Expected: 2 (both calls should go through the custom handler)
            // Actual: 1 (only the first call went through the custom handler)
            assertThat(transactionHandler.getCustomCallCount())
                .as("Both 2-arg and 3-arg inTransaction calls should go through the custom 2-arg override")
                .isEqualTo(2);
        }
    }

    /**
     * A custom transaction handler that only overrides the 2-arg inTransaction method, expecting all transaction calls to go through it for custom
     * housekeeping.
     */
    private static class CustomTransactionHandler extends DelegatingTransactionHandler {
        private final AtomicInteger twoArgCallCount = new AtomicInteger(0);

        private CustomTransactionHandler(TransactionHandler delegate) {
            super(delegate);
        }

        @Override
        public <R, X extends Exception> R inTransaction(Handle handle,
            HandleCallback<R, X> callback) throws X {
            // Custom housekeeping code that users expect to run on every transaction
            twoArgCallCount.incrementAndGet();
            return super.inTransaction(handle, callback);
        }

        // NOTE: We intentionally DO NOT override the 3-arg version, expecting that
        // it will delegate to the 2-arg version as it does in LocalTransactionHandler.
        // This is the bug - when @Transaction annotation is used, it calls the 3-arg version
        // which directly delegates to the wrapped handler, bypassing our custom logic.

        public int getCustomCallCount() {
            return twoArgCallCount.get();
        }
    }

    private static class WorkaroundTransactionHandler extends CustomTransactionHandler {

        private WorkaroundTransactionHandler(TransactionHandler delegate) {
            super(delegate);
        }

        @Override
        public <R, X extends Exception> R inTransaction(Handle handle,
            TransactionIsolationLevel level,
            HandleCallback<R, X> callback) throws X {
            // WORKAROUND: Delegate to the 2-arg version to ensure custom logic is called
            final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();

            try {
                handle.setTransactionIsolationLevel(level);
                return inTransaction(handle, callback);
            } finally {
                handle.setTransactionIsolationLevel(initial);
            }
        }
    }
}

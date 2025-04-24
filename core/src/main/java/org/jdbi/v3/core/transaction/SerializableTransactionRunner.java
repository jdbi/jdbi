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

import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.core.internal.exceptions.Sneaky;

/**
 * A TransactionHandler that automatically retries transactions that fail due to
 * serialization failures, which can generally be resolved by automatically
 * retrying the transaction.  Any HandleCallback used under this runner
 * should be aware that it may be invoked multiple times and should be idempotent.
 */
public class SerializableTransactionRunner extends DelegatingTransactionHandler implements TransactionHandler {
    /* http://www.postgresql.org/docs/9.1/static/errcodes-appendix.html */
    private static final String SQLSTATE_TXN_SERIALIZATION_FAILED = "40001";

    public SerializableTransactionRunner() {
        this(LocalTransactionHandler.binding());
    }

    public SerializableTransactionRunner(TransactionHandler delegate) {
        super(delegate);
    }

    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    HandleCallback<R, X> callback) throws X {
        final Configuration config = handle.getConfig(Configuration.class);
        int attempts = 1 + config.maxRetries;

        Deque<Exception> failures = new ArrayDeque<>();
        while (true) {
            try {
                R result = getDelegate().inTransaction(handle, callback);
                config.onSuccess.accept(new ArrayList<>(failures));
                return result;
            } catch (Exception last) {
                // throw immediately if the exception is unexpected
                if (!isSqlState(config.serializationFailureSqlState, last)) {
                    throw last;
                }

                failures.addLast(last);
                config.onFailure.accept(new ArrayList<>(failures));

                // no more attempts left? Throw ALL the exceptions! \o/
                attempts -= 1;
                if (attempts <= 0) {
                    Exception toThrow = failures.removeLast();
                    while (!failures.isEmpty()) {
                        toThrow.addSuppressed(failures.removeLast());
                    }
                    throw Sneaky.throwAnyway(toThrow);
                }
            }
        }
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    TransactionIsolationLevel level,
                                                    HandleCallback<R, X> callback) throws X {
        final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
        try {
            handle.setTransactionIsolationLevel(level);
            return inTransaction(handle, callback);
        } finally {
            handle.setTransactionIsolationLevel(initial);
        }
    }

    @Override
    public TransactionHandler specialize(Handle handle) throws SQLException {
        return new SerializableTransactionRunner(getDelegate().specialize(handle));
    }

    /**
     * Checks whether a given exception is in a specific SQL state.
     *
     * @param expectedSqlState The expected SQL state.
     * @param throwable The Throwable to test.
     * @return True if Throwable or one of its causes is an SQLException whose SQLState begins with the given state.
     */
    protected boolean isSqlState(String expectedSqlState, Throwable throwable) {
        Throwable t = throwable;

        do {
            if (t instanceof SQLException) {
                String sqlState = ((SQLException) t).getSQLState();

                if (sqlState != null && sqlState.startsWith(expectedSqlState)) {
                    return true;
                }
            }
        } while ((t = t.getCause()) != null);

        return false;
    }

    /**
     * Configuration for serializable transaction runner.
     */
    public static class Configuration implements JdbiConfig<Configuration> {
        private static final int DEFAULT_MAX_RETRIES = 5;

        @SuppressWarnings("UnnecessaryLambda") // constant for readablity
        private static final Consumer<List<Exception>> NOP = list -> {};

        private int maxRetries = DEFAULT_MAX_RETRIES;
        private String serializationFailureSqlState = SQLSTATE_TXN_SERIALIZATION_FAILED;
        private Consumer<List<Exception>> onFailure = NOP;
        private Consumer<List<Exception>> onSuccess = NOP;

        public Configuration() {}

        private Configuration(Configuration that) {
            maxRetries = that.maxRetries;
            serializationFailureSqlState = that.serializationFailureSqlState;
            onFailure = that.onFailure;
            onSuccess = that.onSuccess;
        }

        /**
         * Sets the maximum number of retry attempts before aborting.
         *
         * @param maxRetries The maximum number of retry attempts before aborting.
         * @return this
         */
        public Configuration setMaxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("\"" + maxRetries + " retries\" makes no sense. Set a number >= 0 (default " + DEFAULT_MAX_RETRIES + ").");
            }

            this.maxRetries = maxRetries;
            return this;
        }

        /**
         * Sets the SQL state to consider as a serialization failure.
         *
         * @param serializationFailureSqlState the SQL state to consider as a serialization failure.
         * @return this
         */
        public Configuration setSerializationFailureSqlState(String serializationFailureSqlState) {
            this.serializationFailureSqlState = serializationFailureSqlState;
            return this;
        }

        /**
         * Set a consumer that is called with a list of exceptions during a run. Will not be called with any exceptions that are not the configured
         * serialization failure. These will simply be thrown, aborting the operation. Can be used e.g. for logging.
         *
         * @param onFailure A consumer to handle failures. Will never be called with Exceptions that have not been configured.
         * @return this
         */
        public Configuration setOnFailure(Consumer<List<Exception>> onFailure) {
            this.onFailure = onFailure;
            return this;
        }

        /**
         * Sets a consumer that is called after a run has completed successfully. The consumer will received any exceptions that happened during the run. Will
         * not be called with any exceptions that are not the configured serialization failure. This can be used to e.g. log all exceptions after a successful
         * run.
         *
         * @param onSuccess A consumer to handle the list of failures after the run has been completed successfully.
         * @return this
         */
        public Configuration setOnSuccess(Consumer<List<Exception>> onSuccess) {
            this.onSuccess = onSuccess;
            return this;
        }

        @Override
        public Configuration createCopy() {
            return new Configuration(this);
        }
    }
}

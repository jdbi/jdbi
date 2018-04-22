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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.config.JdbiConfig;

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
        this(new LocalTransactionHandler());
    }

    public SerializableTransactionRunner(TransactionHandler delegate) {
        super(delegate);
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    HandleCallback<R, X> callback) throws X {
        final Configuration config = handle.getConfig(Configuration.class);
        int attempts = 1 + config.maxRetries;

        X stack = null;
        while (true) {
            try {
                return getDelegate().inTransaction(handle, callback);
            } catch (Exception last) {
                X x = (X) last;

                // throw immediately if the exception is unexpected
                if (!isSqlState(config.serializationFailureSqlState, x)) {
                    throw last;
                }

                // keep all exceptions thrown in the loop as a stack
                if (stack == null) {
                    stack = x;
                } else {
                    stack.addSuppressed(last);
                }

                // no more attempts left? Throw ALL the exceptions! \o/
                if (--attempts <= 0) {
                    throw stack;
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
            handle.setTransactionIsolation(level);
            return inTransaction(handle, callback);
        } finally {
            handle.setTransactionIsolation(initial);
        }
    }

    /**
     * @param expectedSqlState the expected SQL state
     * @param throwable the Throwable to test
     * @return whether the Throwable or one of its causes is an SQLException whose SQLState begins with the given state.
     */
    protected boolean isSqlState(String expectedSqlState, Throwable throwable) {
        do {
            if (throwable instanceof SQLException) {
                String sqlState = ((SQLException) throwable).getSQLState();

                if (sqlState != null && sqlState.startsWith(expectedSqlState)) {
                    return true;
                }
            }
        } while ( (throwable = throwable.getCause()) != null);

        return false;
    }

    /**
     * Configuration for serializable transaction runner
     */
    public static class Configuration implements JdbiConfig<Configuration> {
        private static final int DEFAULT_MAX_RETRIES = 5;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private String serializationFailureSqlState = SQLSTATE_TXN_SERIALIZATION_FAILED;

        /**
         * @param maxRetries number of retry attempts before aborting
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
         * @param serializationFailureSqlState the SQL state to consider as a serialization failure
         * @return this
         */
        public Configuration setSerializationFailureSqlState(String serializationFailureSqlState) {
            this.serializationFailureSqlState = serializationFailureSqlState;
            return this;
        }

        @Override
        public Configuration createCopy() {
            return new Configuration()
                    .setMaxRetries(maxRetries)
                    .setSerializationFailureSqlState(serializationFailureSqlState);
        }
    }
}

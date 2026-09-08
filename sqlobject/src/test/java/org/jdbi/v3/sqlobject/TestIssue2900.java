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
package org.jdbi.v3.sqlobject;

import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.transaction.DelegatingTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.jdbi.v3.testing.junit5.internal.TestingInitializers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A method annotated with {@code @Transaction} must run through a custom transaction handler
 * that overrides only the two-argument {@code inTransaction} method.
 *
 * @see <a href="https://github.com/jdbi/jdbi/issues/2900">issue 2900</a>
 */
public class TestIssue2900 {

    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2()
            .withPlugin(new SqlObjectPlugin())
            .withInitializer(TestingInitializers.something());

    private Jdbi jdbi;
    private CountingTransactionHandler countingHandler;

    @BeforeEach
    public void setUp() {
        jdbi = h2Extension.getJdbi();
        countingHandler = new CountingTransactionHandler(jdbi.getTransactionHandler());
        jdbi.setTransactionHandler(countingHandler);
    }

    @Test
    public void annotationWithoutLevelUsesCustomHandler() {
        jdbi.useExtension(Dao.class, Dao::countWithAnnotation);
        assertThat(countingHandler.count()).isEqualTo(1);

        jdbi.useTransaction(handle -> handle.attach(Dao.class).countWithAnnotation());
        assertThat(countingHandler.count()).isEqualTo(2);
    }

    @Test
    public void annotationWithLevelUsesCustomHandler() {
        jdbi.useExtension(Dao.class, Dao::countSerializable);
        assertThat(countingHandler.count()).isEqualTo(1);
    }

    public interface Dao {

        @SqlQuery("select count(*) from something")
        @Transaction
        int countWithAnnotation();

        @SqlQuery("select count(*) from something")
        @Transaction(TransactionIsolationLevel.SERIALIZABLE)
        int countSerializable();
    }

    public static class CountingTransactionHandler extends DelegatingTransactionHandler {
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

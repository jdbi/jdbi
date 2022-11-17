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
package org.jdbi.v3.core;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.transaction.LocalTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.UnableToManipulateTransactionIsolationLevelException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestHandle {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.withSomething();

    private Handle h;

    @BeforeEach
    public void startUp() {
        this.h = h2Extension.openHandle();
    }

    @AfterEach
    public void tearDown() {
        h.close();
    }

    @Test
    public void testInTransaction() {
        String value = h.inTransaction(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).one().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testSillyNumberOfCallbacks() throws Exception {
        h.execute("insert into something (id, name) values (1, 'Keith')");

        // strangely enough, the compiler can't infer this and thinks the throws is redundant
        String value = h2Extension.getJdbi().<String, Exception>withHandle(handle ->
                handle.inTransaction(handle1 ->
                        handle1.createQuery("select name from something where id = 1").mapTo(String.class).one()));

        assertThat(value).isEqualTo("Keith");
    }

    @SuppressWarnings("resource")
    @Test
    public void testIsClosed() {
        assertThat(h.isClosed()).isFalse();
        h.close();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testMrWinter() {
        assertThat(h.execute("CREATE TABLE \"\u2603\" (pk int primary key)")).isZero();
    }

    @Test
    public void unknownTransactionLevelIsOk() {
        assertThatThrownBy(() -> h.setTransactionIsolation(Integer.MIN_VALUE))
            .isInstanceOf(UnableToManipulateTransactionIsolationLevelException.class);

        assertThatCode(() -> h.setTransactionIsolation(TransactionIsolationLevel.UNKNOWN))
            .doesNotThrowAnyException();
    }

    @Test
    public void testAutocommitFailDoesntLeak() {
        final BoomHandler handler = new BoomHandler();
        h2Extension.getJdbi().setTransactionHandler(handler);

        try (Handle transactionHandle = h2Extension.openHandle()) {
            assertThat(transactionHandle.isClosed()).isFalse();

            handler.failTest = true;
            assertThatThrownBy(transactionHandle::close)
                .isInstanceOf(CloseException.class);

            assertThat(transactionHandle.isClosed()).isTrue();
        }
    }

    @Test
    public void testRollbackFailDoesntLeak() throws Exception {
        final BoomHandler handler = new BoomHandler();
        h2Extension.getJdbi().setTransactionHandler(handler);
        try (Handle transactionHandle = h2Extension.openHandle()) {

            assertThat(transactionHandle.isClosed()).isFalse();

            handler.failRollback = true;
            assertThatThrownBy(() -> transactionHandle.useTransaction(h2 -> h2.execute("insert into true")))
                .isInstanceOf(UnableToCreateStatementException.class);
            assertThat(transactionHandle.isInTransaction())
                .describedAs("rollback failed but handle should still be in transaction").isTrue();

            assertThatThrownBy(transactionHandle::close)
                .isInstanceOf(CloseException.class);
            assertThat(transactionHandle.isClosed()).isTrue();
            assertThat(transactionHandle.getConnection().isClosed()).isTrue();
        }
    }

    @Test
    public void testCommitCallback() throws Exception {
        final AtomicBoolean onCommit = new AtomicBoolean();
        final AtomicBoolean onRollback = new AtomicBoolean();

        h.useTransaction(inner -> {
            inner.afterCommit(() -> onCommit.set(true));
            inner.afterRollback(() -> onRollback.set(true));
        });
        assertThat(onCommit).isTrue();
        assertThat(onRollback).isFalse();

        onCommit.set(false);
        h.useTransaction(inner -> {});
        assertThat(onCommit).isFalse();
    }

    @Test
    public void testNestedTxnCommitCallback() throws Exception {
        final AtomicBoolean onCommit = new AtomicBoolean();
        final AtomicBoolean onRollback = new AtomicBoolean();

        h.useTransaction(outer ->
            outer.useTransaction(inner -> {
                inner.afterCommit(() -> onCommit.set(true));
                inner.afterRollback(() -> onRollback.set(true));
            }));
        assertThat(onCommit).isTrue();
        assertThat(onRollback).isFalse();

        onCommit.set(false);
        h.useTransaction(inner -> {});
        assertThat(onCommit).isFalse();
    }

    @Test
    public void testCommitRollback() throws Exception {
        final AtomicBoolean onCommit = new AtomicBoolean();
        final AtomicBoolean onRollback = new AtomicBoolean();
        h.useTransaction(inner -> {
            inner.afterCommit(() -> onCommit.set(true));
            inner.afterRollback(() -> onRollback.set(true));
            inner.rollback();
        });
        assertThat(onCommit).isFalse();
        assertThat(onRollback).isTrue();

        onRollback.set(false);
        h.useTransaction(Handle::rollback);
        assertThat(onRollback).isFalse();
    }

    @Test
    public void testIssue2065() throws Exception {
        h.begin();
        h.execute("insert into something (id, name) values (1, 'Brian')");
        String value = h.createQuery("select name from something where id = 1").mapToBean(Something.class).one().getName();
        h.commit();

        // close connection between commit and handle close - this may be done by the connection pool or timeout or sth else
        h.getConnection().close();

        h.close();
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testUseHandleUsesSameHandle() {
        Jdbi jdbi = h2Extension.getJdbi();
        jdbi.useHandle(handle1 ->
                jdbi.useHandle(handle2 ->
                        assertThat(handle2).isSameAs(handle1)));
    }

    @Test
    public void testAllNestedOpsSameHandle() {
        Jdbi jdbi = h2Extension.getJdbi();
        jdbi.useHandle(handle1 ->
                jdbi.useTransaction(handle2 ->
                        jdbi.withHandle(handle3 ->
                                jdbi.inTransaction(handle4 -> {
                                assertThat(handle1).isSameAs(handle2);
                                assertThat(handle1).isSameAs(handle3);
                                assertThat(handle1).isSameAs(handle4);
                                return null;
                            }))));
    }

    static class BoomHandler extends LocalTransactionHandler {
        boolean failTest;
        boolean failRollback;

        @Override
        public boolean isInTransaction(Handle handle) {
            maybeFail(failTest);
            return super.isInTransaction(handle);
        }

        @Override
        public void rollback(Handle handle) {
            maybeFail(failRollback);
            super.rollback(handle);
        }

        private void maybeFail(boolean fail) {
            if (fail) {
                throw new TransactionException("connection closed");
            }
        }
    }
}

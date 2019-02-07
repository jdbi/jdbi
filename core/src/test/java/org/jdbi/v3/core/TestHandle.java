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

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;
import org.jdbi.v3.core.transaction.LocalTransactionHandler;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.core.transaction.UnableToManipulateTransactionIsolationLevelException;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestHandle {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withSomething();

    @Test
    public void testInTransaction() {
        Handle h = dbRule.openHandle();

        String value = h.inTransaction(handle -> {
            handle.execute("insert into something (id, name) values (1, 'Brian')");
            return handle.createQuery("select name from something where id = 1").mapToBean(Something.class).findOnly().getName();
        });
        assertThat(value).isEqualTo("Brian");
    }

    @Test
    public void testSillyNumberOfCallbacks() throws Exception {
        try (Handle h = dbRule.openHandle()) {
            h.execute("insert into something (id, name) values (1, 'Keith')");
        }

        // strangely enough, the compiler can't infer this and thinks the throws is redundant
        String value = dbRule.getJdbi().<String, Exception>withHandle(handle ->
                handle.inTransaction(handle1 ->
                        handle1.createQuery("select name from something where id = 1").mapTo(String.class).findOnly()));

        assertThat(value).isEqualTo("Keith");
    }

    @SuppressWarnings("resource")
    @Test
    public void testIsClosed() {
        Handle h = dbRule.openHandle();
        assertThat(h.isClosed()).isFalse();
        h.close();
        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testMrWinter() {
        final Handle h = dbRule.getSharedHandle();
        h.execute("CREATE TABLE \"\u2603\" (pk int primary key)");
    }

    @Test
    public void unknownTransactionLevelIsOk() {
        Handle h = dbRule.openHandle();

        assertThatThrownBy(() -> h.setTransactionIsolation(Integer.MIN_VALUE))
            .isInstanceOf(UnableToManipulateTransactionIsolationLevelException.class);

        assertThatCode(() -> h.setTransactionIsolation(TransactionIsolationLevel.UNKNOWN))
            .doesNotThrowAnyException();
    }

    @Test
    public void testAutocommitFailDoesntLeak() {
        final BoomHandler handler = new BoomHandler();
        dbRule.getJdbi().setTransactionHandler(handler);
        final Handle h = dbRule.openHandle();

        assertThat(h.isClosed()).isFalse();

        handler.failTest = true;
        assertThatThrownBy(h::close)
            .isInstanceOf(CloseException.class);

        assertThat(h.isClosed()).isTrue();
    }

    @Test
    public void testRollbackFailDoesntLeak() throws Exception {
        final BoomHandler handler = new BoomHandler();
        dbRule.getJdbi().setTransactionHandler(handler);
        final Handle h = dbRule.openHandle();

        assertThat(h.isClosed()).isFalse();

        handler.failRollback = true;
        assertThatThrownBy(() -> h.useTransaction(h2 -> h2.execute("insert into true")))
            .isInstanceOf(UnableToCreateStatementException.class);
        assertThat(h.isInTransaction())
            .describedAs("rollback failed but handle should still be in transaction").isTrue();

        assertThatThrownBy(h::close)
            .isInstanceOf(CloseException.class);
        assertThat(h.isClosed()).isTrue();
        assertThat(h.getConnection().isClosed()).isTrue();
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

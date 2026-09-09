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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestLocalTransactionHandler {

    Connection c = Mockito.mock(Connection.class);

    Handle h = Jdbi.create(() -> c).open();

    @Test
    public void testRollbackThrow() throws Exception {
        RuntimeException outer = new RuntimeException("Transaction throws!");
        RuntimeException inner = new RuntimeException("Rollback throws!");

        Mockito.when(c.getAutoCommit()).thenReturn(true);
        Mockito.doThrow(inner).when(c).rollback();

        try {
            new LocalTransactionHandler().inTransaction(h, x -> {
                throw outer;
            });
        } catch (RuntimeException e) {
            assertThat(e).isSameAs(outer);
            assertThat(e.getSuppressed()).hasSize(1);
            assertThat(e.getSuppressed()[0]).isSameAs(inner);
        }
    }

    @Test
    public void testCommitFailureStillFiresAfterRollbackCallback() throws Exception {
        Mockito.when(c.getAutoCommit()).thenReturn(true);
        Mockito.doThrow(new SQLException("commit failed")).when(c).commit();

        AtomicBoolean rolledBack = new AtomicBoolean();

        assertThatThrownBy(() ->
            h.inTransaction(handle -> {
                handle.afterRollback(() -> rolledBack.set(true));
                return null;
            }))
            .isInstanceOf(TransactionException.class);

        assertThat(rolledBack).isTrue();
    }

    @Test
    public void testCommitFailureFiresAfterRollbackWhenAutoCommitDisabled() throws Exception {
        Mockito.when(c.getAutoCommit()).thenReturn(false);
        Mockito.doThrow(new SQLException("commit failed")).when(c).commit();

        AtomicBoolean rolledBack = new AtomicBoolean();
        h.afterRollback(() -> rolledBack.set(true));

        assertThatThrownBy(h::commit).isInstanceOf(TransactionException.class);

        assertThat(rolledBack).isTrue();
    }

    @Test
    public void testSerializationFailureAtCommitFiresAfterRollbackBeforeRetry() throws Exception {
        Mockito.when(c.getAutoCommit()).thenReturn(true);
        Mockito.doThrow(new SQLException("serialization failure", "40001")).doNothing().when(c).commit();

        Jdbi jdbi = Jdbi.create(() -> c);
        jdbi.setTransactionHandler(new SerializableTransactionRunner());

        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger rollbacks = new AtomicInteger();

        try (Handle handle = jdbi.open()) {
            handle.inTransaction(x -> {
                attempts.incrementAndGet();
                x.afterRollback(rollbacks::incrementAndGet);
                return null;
            });
        }

        assertThat(attempts).hasValue(2);
        assertThat(rollbacks).hasValue(1);
    }

    @Test
    public void testThrowError() throws Exception {
        Error error = new Error("Transaction throws!");

        Mockito.when(c.getAutoCommit()).thenReturn(true);

        assertThatThrownBy(() ->
            new LocalTransactionHandler().inTransaction(h, x -> {
                throw error;
            }))
            .isSameAs(error);
    }
}

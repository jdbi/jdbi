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
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The transaction callback queue must drain exactly once per transaction, as afterCommit when
 * the connection committed and as afterRollback otherwise, even when the JDBC connection fails
 * during the rollback or during the autocommit restore that follows a commit.
 */
public class TestTransactionCallbackDrain {

    Connection c = Mockito.mock(Connection.class);

    AtomicInteger commits = new AtomicInteger();
    AtomicInteger rollbacks = new AtomicInteger();

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(c.getAutoCommit()).thenReturn(true);
    }

    private void register(Handle handle) {
        handle.afterCommit(commits::incrementAndGet);
        handle.afterRollback(rollbacks::incrementAndGet);
    }

    @Test
    public void rollbackFailureAfterCommitFailureFiresAfterRollbackOnce() throws Exception {
        Mockito.doThrow(new SQLException("commit failed")).doNothing().when(c).commit();
        Mockito.doThrow(new SQLException("rollback failed")).doNothing().when(c).rollback();

        try (Handle handle = Jdbi.create(() -> c).open()) {
            assertThatThrownBy(() -> handle.useTransaction(this::register))
                .isInstanceOf(TransactionException.class)
                .satisfies(e -> assertThat(e.getCause().getSuppressed()).hasSize(1));

            assertThat(rollbacks).hasValue(1);
            assertThat(commits).hasValue(0);

            handle.useTransaction(txn -> {});
            handle.useTransaction(Handle::rollback);
        }

        assertThat(rollbacks).hasValue(1);
        assertThat(commits).hasValue(0);
    }

    @Test
    public void explicitRollbackFailureFiresAfterRollbackOnce() throws Exception {
        Mockito.doThrow(new SQLException("rollback failed")).doNothing().when(c).rollback();

        try (Handle handle = Jdbi.create(() -> c).open()) {
            assertThatThrownBy(() -> handle.useTransaction(txn -> {
                register(txn);
                txn.rollback();
            })).isInstanceOf(TransactionException.class);

            assertThat(rollbacks).hasValue(1);
            assertThat(commits).hasValue(0);

            handle.useTransaction(txn -> {});
            handle.useTransaction(Handle::rollback);
        }

        assertThat(rollbacks).hasValue(1);
        assertThat(commits).hasValue(0);
    }

    @Test
    public void autoCommitRestoreFailureAfterCommitFiresAfterCommitOnce() throws Exception {
        Mockito.doNothing().doThrow(new SQLException("restore failed")).doNothing().when(c).setAutoCommit(Mockito.anyBoolean());

        try (Handle handle = Jdbi.create(() -> c).open()) {
            assertThatThrownBy(() -> handle.useTransaction(this::register))
                .isInstanceOf(UnableToRestoreAutoCommitStateException.class);

            Mockito.verify(c).commit();
            assertThat(commits).hasValue(1);
            assertThat(rollbacks).hasValue(0);

            handle.useTransaction(Handle::rollback);
            handle.useTransaction(txn -> {});
        }

        assertThat(commits).hasValue(1);
        assertThat(rollbacks).hasValue(0);
    }

    @Test
    public void autoCommitRestoreFailureAfterRollbackFiresAfterRollbackOnce() throws Exception {
        Mockito.doNothing().doThrow(new SQLException("restore failed")).doNothing().when(c).setAutoCommit(Mockito.anyBoolean());

        try (Handle handle = Jdbi.create(() -> c).open()) {
            assertThatThrownBy(() -> handle.useTransaction(txn -> {
                register(txn);
                txn.rollback();
            })).isInstanceOf(UnableToRestoreAutoCommitStateException.class);

            Mockito.verify(c).rollback();
            assertThat(rollbacks).hasValue(1);
            assertThat(commits).hasValue(0);

            handle.useTransaction(txn -> {});
        }

        assertThat(rollbacks).hasValue(1);
        assertThat(commits).hasValue(0);
    }
}

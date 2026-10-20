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
import java.sql.DriverManager;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestNoOpTransactionHandler {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    @Test
    public void testNeverTouchesConnectionTransactionState() throws Exception {
        Connection c = Mockito.mock(Connection.class);
        Jdbi jdbi = Jdbi.create(() -> c);
        jdbi.setTransactionHandler(new NoOpTransactionHandler());

        try (Handle handle = jdbi.open()) {
            handle.begin();
            handle.commit();
            assertThat(handle.isInTransaction()).isFalse();
            handle.useTransaction(txn -> {});
        }

        Mockito.verify(c, Mockito.never()).getAutoCommit();
        Mockito.verify(c, Mockito.never()).setAutoCommit(Mockito.anyBoolean());
        Mockito.verify(c, Mockito.never()).commit();
        Mockito.verify(c, Mockito.never()).rollback();
    }

    @Test
    public void testInTransactionDoesNotCommit() throws Exception {
        h2Extension.getJdbi().useHandle(h -> h.execute("create table names(name varchar)"));

        Jdbi jdbi = Jdbi.create(() -> {
            Connection connection = DriverManager.getConnection(h2Extension.getUri());
            connection.setAutoCommit(false);
            return connection;
        });
        jdbi.setTransactionHandler(new NoOpTransactionHandler());

        try (Handle handle = jdbi.open()) {
            handle.useTransaction(txn -> txn.execute("insert into names (name) values ('Kafka')"));
            assertThat(committedRows()).isZero();

            // the test acts as the external transaction manager
            handle.getConnection().commit();
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    @Test
    public void testRollbackThrows() {
        Connection c = Mockito.mock(Connection.class);
        Jdbi jdbi = Jdbi.create(() -> c);
        jdbi.setTransactionHandler(new NoOpTransactionHandler());

        try (Handle handle = jdbi.open()) {
            assertThatThrownBy(handle::rollback).isInstanceOf(TransactionException.class);
        }
    }

    @Test
    public void testCallbackRegistrationRejected() {
        Connection c = Mockito.mock(Connection.class);
        Jdbi jdbi = Jdbi.create(() -> c);
        jdbi.setTransactionHandler(new NoOpTransactionHandler());

        try (Handle handle = jdbi.open()) {
            assertThatThrownBy(() -> handle.afterCommit(() -> {})).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> handle.afterRollback(() -> {})).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testCmtReadsAutoCommitFlag() throws Exception {
        Connection c = Mockito.mock(Connection.class);
        Mockito.when(c.getAutoCommit()).thenReturn(false);
        Jdbi jdbi = Jdbi.create(() -> c);
        jdbi.setTransactionHandler(new CMTTransactionHandler());

        try (Handle handle = jdbi.open()) {
            assertThat(handle.isInTransaction()).isTrue();
        }
    }

    private int committedRows() {
        return h2Extension.getJdbi().withHandle(
            h -> h.createQuery("select count(1) from names").mapTo(Integer.class).one());
    }
}

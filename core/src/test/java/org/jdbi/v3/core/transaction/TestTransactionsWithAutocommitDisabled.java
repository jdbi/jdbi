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
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documents the transaction contract for a connection with autocommit already
 * disabled: the handle joins the transaction that the connection owner
 * manages. See the "Transactions managed outside Jdbi" section of the User
 * Guide.
 */
public class TestTransactionsWithAutocommitDisabled {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance();

    private Jdbi jdbi;

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().useHandle(h -> h.execute("create table names(name varchar)"));
        jdbi = Jdbi.create(() -> {
            Connection connection = DriverManager.getConnection(h2Extension.getUri());
            connection.setAutoCommit(false);
            return connection;
        });
    }

    @Test
    public void testHandleReportsOpenTransaction() {
        try (Handle handle = jdbi.open()) {
            assertThat(handle.isInTransaction()).isTrue();
        }
    }

    @Test
    public void testInTransactionJoinsAndDoesNotCommit() {
        try (Handle handle = jdbi.open()) {
            handle.useTransaction(txn -> txn.execute("insert into names (name) values ('Kafka')"));
            assertThat(committedRows()).isZero();

            handle.commit();
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    @Test
    public void testExplicitTransactionCycles() {
        try (Handle handle = jdbi.open()) {
            handle.begin();
            handle.execute("insert into names (name) values ('Kafka')");
            handle.commit();
            assertThat(committedRows()).isEqualTo(1);

            handle.execute("insert into names (name) values ('Camus')");
            handle.rollback();
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    @Test
    public void testDifferentIsolationLevelRejected() {
        try (Handle handle = jdbi.open()) {
            assertThatThrownBy(() -> handle.useTransaction(TransactionIsolationLevel.SERIALIZABLE, txn -> {}))
                .isInstanceOf(TransactionException.class);

            assertThatCode(() -> handle.useTransaction(TransactionIsolationLevel.UNKNOWN, txn -> {}))
                .doesNotThrowAnyException();
        }
    }

    @Test
    public void testCloseDoesNotThrowAndDoesNotCommit() {
        Handle handle = jdbi.open();
        handle.execute("insert into names (name) values ('Kafka')");

        assertThatCode(handle::close).doesNotThrowAnyException();
        assertThat(committedRows()).isZero();
    }

    @Test
    public void testCallbacksDrainPerCycle() {
        try (Handle handle = jdbi.open()) {
            AtomicInteger commits = new AtomicInteger();
            AtomicInteger rollbacks = new AtomicInteger();

            handle.afterCommit(commits::incrementAndGet);
            handle.commit();
            assertThat(commits).hasValue(1);

            handle.afterRollback(rollbacks::incrementAndGet);
            handle.rollback();
            assertThat(commits).hasValue(1);
            assertThat(rollbacks).hasValue(1);
        }
    }

    private int committedRows() {
        return h2Extension.getJdbi().withHandle(
            h -> h.createQuery("select count(1) from names").mapTo(Integer.class).one());
    }
}

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
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.junit5.H2DatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Documents {@link LocalTransactionHandler#managed()}: Jdbi manages
 * transactions on a connection with autocommit already disabled, instead of
 * joining the transaction of the connection owner.
 */
public class TestManagedTransactionsWithAutocommitDisabled {

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
        jdbi.setTransactionHandler(LocalTransactionHandler.managed());
    }

    @Test
    public void testHandleReportsNoOpenTransaction() {
        try (Handle handle = jdbi.open()) {
            assertThat(handle.isInTransaction()).isFalse();
        }
    }

    @Test
    public void testInTransactionCommits() {
        try (Handle handle = jdbi.open()) {
            handle.useTransaction(txn -> txn.execute("insert into names (name) values ('Kafka')"));
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    @Test
    public void testInTransactionRollsBackOnException() {
        try (Handle handle = jdbi.open()) {
            assertThatThrownBy(() -> handle.useTransaction(txn -> {
                txn.execute("insert into names (name) values ('Kafka')");
                throw new IllegalStateException("boom");
            })).isInstanceOf(IllegalStateException.class);

            assertThat(committedRows()).isZero();
        }
    }

    @Test
    public void testManualBeginCommit() {
        try (Handle handle = jdbi.open()) {
            handle.begin();
            assertThat(handle.isInTransaction()).isTrue();

            handle.execute("insert into names (name) values ('Kafka')");
            handle.commit();

            assertThat(handle.isInTransaction()).isFalse();
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    @Test
    public void testCloseInsideOpenTransactionThrows() {
        Handle handle = jdbi.open();
        handle.begin();
        handle.execute("insert into names (name) values ('Kafka')");

        assertThatThrownBy(handle::close).isInstanceOf(TransactionException.class);
        assertThat(committedRows()).isZero();
    }

    @Test
    public void testStatementWithoutTransactionIsNotCommitted() {
        Handle handle = jdbi.open();
        handle.execute("insert into names (name) values ('Kafka')");
        handle.close();

        assertThat(committedRows()).isZero();
    }

    @Test
    public void testIsolationLevelVariantWorks() {
        try (Handle handle = jdbi.open()) {
            handle.useTransaction(TransactionIsolationLevel.SERIALIZABLE,
                txn -> txn.execute("insert into names (name) values ('Kafka')"));
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    @Test
    public void testSerializableTransactionRunnerEngages() throws Exception {
        jdbi.setTransactionHandler(new SerializableTransactionRunner(LocalTransactionHandler.managed()));

        AtomicInteger attempts = new AtomicInteger();
        try (Handle handle = jdbi.open()) {
            handle.useTransaction(TransactionIsolationLevel.SERIALIZABLE, txn -> {
                txn.execute("insert into names (name) values ('Kafka')");
                if (attempts.incrementAndGet() == 1) {
                    throw new SQLException("serialization failure", "40001");
                }
            });
        }

        assertThat(attempts).hasValue(2);
        assertThat(committedRows()).isEqualTo(1);
    }

    @Test
    public void testNestedInTransactionJoins() {
        try (Handle handle = jdbi.open()) {
            handle.useTransaction(outer -> {
                outer.execute("insert into names (name) values ('Kafka')");
                outer.useTransaction(inner -> inner.execute("insert into names (name) values ('Camus')"));
                assertThat(committedRows()).isZero();
            });
            assertThat(committedRows()).isEqualTo(2);
        }
    }

    @Test
    public void testCallbacksFire() {
        try (Handle handle = jdbi.open()) {
            AtomicInteger commits = new AtomicInteger();
            AtomicInteger rollbacks = new AtomicInteger();

            handle.useTransaction(txn -> {
                txn.afterCommit(commits::incrementAndGet);
                txn.afterRollback(rollbacks::incrementAndGet);
            });

            assertThat(commits).hasValue(1);
            assertThat(rollbacks).hasValue(0);
        }
    }

    @Test
    public void testManagedHandlerWithAutocommitEnabled() {
        Jdbi autocommitJdbi = Jdbi.create(() -> DriverManager.getConnection(h2Extension.getUri()));
        autocommitJdbi.setTransactionHandler(LocalTransactionHandler.managed());

        try (Handle handle = autocommitJdbi.open()) {
            assertThat(handle.isInTransaction()).isFalse();

            handle.useTransaction(txn -> txn.execute("insert into names (name) values ('Kafka')"));
            assertThat(committedRows()).isEqualTo(1);

            handle.begin();
            assertThat(handle.isInTransaction()).isTrue();
            handle.execute("insert into names (name) values ('Camus')");
            handle.rollback();
            assertThat(committedRows()).isEqualTo(1);
        }
    }

    private int committedRows() {
        return h2Extension.getJdbi().withHandle(
            h -> h.createQuery("select count(1) from names").mapTo(Integer.class).one());
    }
}

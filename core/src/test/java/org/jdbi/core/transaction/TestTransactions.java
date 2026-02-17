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
package org.jdbi.core.transaction;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.internal.testing.H2DatabaseExtension;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.TemplateEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestTransactions {

    @RegisterExtension
    public H2DatabaseExtension h2Extension = H2DatabaseExtension.instance().withInitializer(H2DatabaseExtension.SOMETHING_INITIALIZER);

    int begin, commit, rollback;

    private Handle h;

    private final LocalTransactionHandler txSpy = new LocalTransactionHandler() {
        @Override
        public void begin(Handle handle) {
            begin++;
            super.begin(handle);
        }

        @Override
        public void commit(Handle handle) {
            commit++;
            super.commit(handle);
        }

        @Override
        public void rollback(Handle handle) {
            rollback++;
            super.rollback(handle);
        }
    };

    @BeforeEach
    public void setUp() {
        h2Extension.getJdbi().setTransactionHandler(txSpy);
        h = h2Extension.openHandle();
    }

    @AfterEach
    public void close() {
        h.close();
    }

    @Test
    public void testCallback() {
        String woot = h.inTransaction(x -> "Woot!");

        assertThat(woot).isEqualTo("Woot!");
    }

    @Test
    public void testRollbackOutsideTx() {
        h.execute("insert into something (id, name) values (?, ?)", 7, "Tom");
        assertThat(h.isInTransaction()).isFalse();
        h.rollback();
        assertThat(h.isInTransaction()).isFalse();
    }

    @Test
    public void testDoubleOpen() throws Exception {
        assertThat(h.getConnection().getAutoCommit()).isTrue();

        h.begin();
        h.begin();
        assertThat(h.getConnection().getAutoCommit()).isFalse();
        h.commit();
        assertThat(h.getConnection().getAutoCommit()).isTrue();
    }

    @Test
    public void testNoBeginTransaction() throws Exception {

        Jdbi jdbi = Jdbi.create(() -> {
            // create connection with auto-commit == false
            Connection connection = DriverManager.getConnection(h2Extension.getUri());
            connection.setAutoCommit(false);
            return connection;
        });

        jdbi.useTransaction(handle -> {
            assertThat(handle.getConnection().getAutoCommit()).isFalse();
            handle.execute("INSERT INTO something (id, name ) VALUES (1, 'foo')");
            handle.commit();
        });

        int result = h.createQuery("SELECT count(1) from something")
            .mapTo(Integer.class)
            .one();
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testExceptionAbortsTransaction() {
        assertThatThrownBy(() ->
            h.inTransaction(handle -> {
                handle.execute("insert into something (id, name) values (?, ?)", 0, "Keith");
                throw new IOException();
            }))
            .isInstanceOf(IOException.class);

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertThat(r).isEmpty();
    }

    @Test
    public void testExceptionAbortsUseTransaction() {
        assertThatThrownBy(() ->
            h.useTransaction(handle -> {
                handle.execute("insert into something (id, name) values (?, ?)", 0, "Keith");
                throw new IOException();
            }))
            .isInstanceOf(IOException.class);

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertThat(r).isEmpty();
    }

    @Test
    public void testRollbackDoesntCommit() {
        assertThat(begin).isZero();
        h.useTransaction(th -> {
            assertThat(begin).isOne();
            assertThat(rollback).isZero();
            th.rollback();
        });
        assertThat(rollback).isOne();
        assertThat(commit).isZero();
    }

    @Test
    public void testSavepoint() {
        h.begin();

        h.execute("insert into something (id, name) values (?, ?)", 1, "Tom");
        h.savepoint("first");
        h.execute("insert into something (id, name) values (?, ?)", 2, "Martin");
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).one())
            .isEqualTo(Integer.valueOf(2));
        h.rollbackToSavepoint("first");
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).one())
            .isEqualTo(Integer.valueOf(1));
        h.commit();
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).one())
            .isEqualTo(Integer.valueOf(1));
    }

    @Test
    public void testReleaseSavepoint() {
        h.begin();
        h.savepoint("first");
        h.execute("insert into something (id, name) values (?, ?)", 1, "Martin");

        h.releaseSavepoint("first");

        assertThatExceptionOfType(TransactionException.class)
            .isThrownBy(() -> h.rollbackToSavepoint("first"));

        h.rollback();
    }

    @Test
    public void testThrowingRuntimeExceptionPercolatesOriginal() {
        assertThatThrownBy(() -> h.inTransaction(handle -> {
            throw new IllegalArgumentException();
        })).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testTemplateEngineThrowsError() {
        assertThatThrownBy(() -> h.setTemplateEngine(new BoomEngine()).inTransaction(h2 -> h2.execute("select 1")))
            .isOfAnyClassIn(Error.class)
            .hasMessage("boom");
        assertThat(h.isInTransaction()).isFalse();
    }

    @Test
    public void commitThrowsDoesntCommit() throws SQLException {
        h.execute("create table commitTable (id int primary key)");

        var forwardAnswer = AdditionalAnswers.delegatesTo(h.getConnection());
        var c = Mockito.mock(Connection.class, Mockito.withSettings()
                .defaultAnswer(forwardAnswer));

        var jdbi = Jdbi.create(c);

        var expectedExn = new SQLException("woof");
        Mockito.doThrow(expectedExn).when(c).commit();
        assertThatThrownBy(() -> jdbi.useTransaction(txn -> txn.execute("insert into commitTable(id) values (1)")))
            .hasCause(expectedExn);

        assertThat(h.createQuery("select count(1) from commitTable")
                .mapTo(int.class)
                .one())
            .isZero();
    }

    static class BoomEngine implements TemplateEngine {

        @Override
        public String render(String template, StatementContext ctx) {
            throw new Error("boom");
        }
    }
}

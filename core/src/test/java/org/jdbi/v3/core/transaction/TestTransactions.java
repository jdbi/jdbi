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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.util.List;

import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestTransactions
{
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule();

    int begin, commit, rollback;

    private Handle h;

    private final LocalTransactionHandler txSpy = new LocalTransactionHandler()
    {
        @Override
        public void begin(Handle handle)
        {
            begin++;
            super.begin(handle);
        }

        @Override
        public void commit(Handle handle)
        {
            commit++;
            super.commit(handle);
        }

        @Override
        public void rollback(Handle handle)
        {
            rollback++;
            super.rollback(handle);
        }
    };

    @Before
    public void setUp()
    {
        dbRule.getJdbi().setTransactionHandler(txSpy);
        h = dbRule.openHandle();
    }

    @After
    public void close()
    {
        h.close();
    }

    @Test
    public void testCallback() throws Exception
    {
        String woot = h.inTransaction(x -> "Woot!");

        assertThat(woot).isEqualTo("Woot!");
    }

    @Test
    public void testRollbackOutsideTx() throws Exception
    {
        h.insert("insert into something (id, name) values (?, ?)", 7, "Tom");
        h.rollback();
    }

    @Test
    public void testDoubleOpen() throws Exception
    {
        assertThat(h.getConnection().getAutoCommit()).isTrue();

        h.begin();
        h.begin();
        assertThat(h.getConnection().getAutoCommit()).isFalse();
        h.commit();
        assertThat(h.getConnection().getAutoCommit()).isTrue();
    }

    @Test
    public void testExceptionAbortsTransaction() throws Exception
    {
        assertThatExceptionOfType(IOException.class).isThrownBy(() ->
                h.inTransaction(handle -> {
                    handle.insert("insert into something (id, name) values (:id, :name)", 0, "Keith");
                    throw new IOException();
                }));

        List<Something> r = h.createQuery("select * from something").mapToBean(Something.class).list();
        assertThat(r).isEmpty();
    }

    @Test
    public void testRollbackDoesntCommit() throws Exception
    {
        assertThat(begin).isEqualTo(0);
        h.useTransaction(th -> {
            assertThat(begin).isEqualTo(1);
            assertThat(rollback).isEqualTo(0);
            th.rollback();
        });
        assertThat(rollback).isEqualTo(1);
        assertThat(commit).isEqualTo(0);
    }

    @Test
    public void testSavepoint() throws Exception
    {
        h.begin();

        h.insert("insert into something (id, name) values (:id, :name)", 1, "Tom");
        h.savepoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 2, "Martin");
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly())
                .isEqualTo(Integer.valueOf(2));
        h.rollbackToSavepoint("first");
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly())
                .isEqualTo(Integer.valueOf(1));
        h.commit();
        assertThat(h.createQuery("select count(*) from something").mapTo(Integer.class).findOnly())
                .isEqualTo(Integer.valueOf(1));
    }

    @Test
    public void testReleaseSavepoint() throws Exception
    {
        h.begin();
        h.savepoint("first");
        h.insert("insert into something (id, name) values (:id, :name)", 1, "Martin");

        h.release("first");

        assertThatExceptionOfType(TransactionException.class)
                .isThrownBy(() -> h.rollbackToSavepoint("first"));

        h.rollback();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowingRuntimeExceptionPercolatesOriginal() throws Exception
    {
        h.inTransaction(handle -> {
            throw new IllegalArgumentException();
        });
    }
}

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class TestTransactionAnnotation {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
    }

    @Test
    public void testTx() {
        Dao dao = handle.attach(Dao.class);
        Something s = dao.insertAndFetch(1, "Ian");
        assertThat(s).isEqualTo(new Something(1, "Ian"));
    }

    @Test
    public void testTxFail() {
        Dao dao = handle.attach(Dao.class);

        Throwable ex = catchThrowable(() -> dao.fail(1, "Ian"));

        assertThat(ex).isInstanceOf(UncheckedIOException.class);
        assertThat(ex.getCause())
            .isInstanceOf(IOException.class)
            .hasMessage("woof");

        assertThat(dao.findById(1)).isNull();
    }

    @Test
    public void testTxActuallyCommits() {
        Handle h2 = this.dbRule.openHandle();
        Dao one = handle.attach(Dao.class);
        Dao two = h2.attach(Dao.class);

        // insert in @Transaction method
        Something inserted = one.insertAndFetch(1, "Brian");

        // fetch from another connection
        Something fetched = two.findById(1);
        assertThat(fetched).isEqualTo(inserted);
    }

    @Test
    public void testConcurrent() throws Exception {
        ExecutorService es = Executors.newFixedThreadPool(3);

        final CountDownLatch inserted = new CountDownLatch(1);
        final CountDownLatch committed = new CountDownLatch(1);

        final Other o = dbRule.getJdbi().onDemand(Other.class);
        Future<Void> rf = es.submit(Unchecked.callable(() -> {
            o.insert(inserted, 1, "diwaker");
            committed.countDown();
            return null;
        }));

        Future<Void> tf = es.submit(Unchecked.callable(() -> {
            inserted.await();
            committed.await();

            Something s2 = o.find(1);
            assertThat(s2).isEqualTo(new Something(1, "diwaker"));
            return null;
        }));

        rf.get();
        tf.get();

        es.shutdown();
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Other {
        @Transaction
        default void insert(CountDownLatch inserted, int id, String name) {
            reallyInsert(id, name);
            inserted.countDown();
        }

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void reallyInsert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something find(@Bind("id") int id);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        default Something insertAndFetch(int id, String name) {
            insert(id, name);
            return findById(id);
        }

        @Transaction
        default Something fail(int id, String name) throws IOException {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}

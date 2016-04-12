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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class TestTransactionAnnotation
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();
    }

    @Test
    public void testTx() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
        Something s = dao.insertAndFetch(1, "Ian");
        assertThat(s, equalTo(new Something(1, "Ian")));
    }

    @Test
    public void testTxFail() throws Exception
    {
        Dao dao = handle.attach(Dao.class);

        exception.expectMessage("woof");
        try {
            dao.failed(1, "Ian");
        }
        finally {
            assertThat(dao.findById(1), nullValue());
        }
    }

    @Test
    public void testTxActuallyCommits() throws Exception
    {
        Handle h2 = db.openHandle();
        Dao one = handle.attach(Dao.class);
        Dao two = h2.attach(Dao.class);

        // insert in @Transaction method
        Something inserted = one.insertAndFetch(1, "Brian");

        // fetch from another connection
        Something fetched = two.findById(1);
        assertThat(fetched, equalTo(inserted));
    }

    @Test
    public void testConcurrent() throws Exception
    {
        ExecutorService es = Executors.newFixedThreadPool(3);

        final CountDownLatch inserted = new CountDownLatch(1);
        final CountDownLatch committed = new CountDownLatch(1);

        final Other o = db.getDbi().onDemand(Other.class);
        Future<Void> rf = es.submit(() -> {
            try {
                o.insert(inserted, 1, "diwaker");
                committed.countDown();

                return null;
            }
            catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
                return null;
            }
        });

        Future<Void> tf = es.submit(() -> {
            try {
                inserted.await();
                committed.await();

                Something s2 = o.find(1);
                assertThat(s2, equalTo(new Something(1, "diwaker")));
                return null;
            }
            catch (Exception e) {
                e.printStackTrace();
                fail(e.getMessage());
                return null;
            }
        });

        rf.get();
        tf.get();

        es.shutdown();
    }

    @RegisterMapper(SomethingMapper.class)
    public interface Other
    {
        @Transaction
        default void insert(CountDownLatch inserted, int id, String name) throws InterruptedException
        {
            reallyInsert(id, name);
            inserted.countDown();
        }

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void reallyInsert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something find(@Bind("id") int id);
    }

    @RegisterMapper(SomethingMapper.class)
    public interface Dao
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        @Transaction(TransactionIsolationLevel.READ_COMMITTED)
        default Something insertAndFetch(int id, String name)
        {
            insert(id, name);
            return findById(id);
        }

        @Transaction
        default Something failed(int id, String name) throws IOException
        {
            insert(id, name);
            throw new IOException("woof");
        }
    }
}

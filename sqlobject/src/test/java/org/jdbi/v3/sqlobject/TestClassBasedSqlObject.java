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
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.hamcrest.CoreMatchers;
import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.exception.TransactionException;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.subpackage.SomethingDao;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class TestClassBasedSqlObject
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
    public void testPassThroughMethod() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c, equalTo(new Something(3, "Cora")));
    }

    @Test
    public void testUnimplementedMethod() throws Exception
    {
        Dao dao = handle.attach(Dao.class);

        exception.expect(AbstractMethodError.class);
        exception.expectCause(instanceOf(AbstractMethodError.class));

        dao.totallyBroken();
    }

    @Test
    public void testPassThroughMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = handle.attach(SomethingDao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c, equalTo(new Something(3, "Cora")));
    }

    @Test(expected = AbstractMethodError.class)
    public void testUnimplementedMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = handle.attach(SomethingDao.class);
        dao.totallyBroken();
    }

    @Test
    public void testSimpleTransactionsSucceed() throws Exception
    {
        SomethingDao dao = db.getJdbi().onDemand(SomethingDao.class);

        dao.insertInSingleTransaction(10, "Linda");
    }

    @Test
    public void testTransactionAnnotationWorksOnInterfaceDefaultMethod() throws Exception
    {
        Dao dao = db.getSharedHandle().attach(Dao.class);
        assertTrue(dao.doesTransactionAnnotationWork());
    }

    @Test
    public void testNestedTransactionsCollapseIntoSingleTransaction()
    {
        Handle handle = Mockito.spy(db.getSharedHandle());
        Dao dao = handle.attach(Dao.class);

        dao.threeNestedTransactions();
        verify(handle, times(1)).begin();
        verify(handle, times(1)).commit();

        dao.twoNestedTransactions();
        verify(handle, times(2)).begin();
        verify(handle, times(2)).commit();
    }

    @Test
    public void testNestedTransactionWithSameIsolation() {
        Handle handle = Mockito.spy(db.getSharedHandle());
        Dao dao = handle.attach(Dao.class);

        dao.nestedTransactionWithSameIsolation();
        verify(handle, times(1)).begin();
        verify(handle, times(1)).commit();
    }

    @Test(expected = TransactionException.class)
    public void testNestedTransactionWithDifferentIsoltion() {
        Handle handle = Mockito.spy(db.getSharedHandle());
        Dao dao = handle.attach(Dao.class);

        dao.nestedTransactionWithDifferentIsolation();
    }

    @Test
    public void testSqlUpdateWithTransaction() {
        Handle handle = Mockito.spy(db.getSharedHandle());
        Dao dao = handle.attach(Dao.class);

        dao.insert(1, "foo");
        verify(handle, never()).begin();
        assertThat(dao.findById(1), equalTo(new Something(1, "foo")));

        dao.insertTransactional(2, "bar");
        verify(handle, times(1)).begin();
        assertThat(dao.findById(2), equalTo(new Something(2, "bar")));
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        @Transaction
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insertTransactional(@Bind("id") int id, @Bind("name") String name);

        default Something findByIdHeeHee(int id) {
            return findById(id);
        }

        void totallyBroken();

        @Transaction
        default void threeNestedTransactions() {
            twoNestedTransactions();
        }

        @Transaction
        default void twoNestedTransactions() {
            assertTrue(doesTransactionAnnotationWork());
        }

        @Transaction
        default boolean doesTransactionAnnotationWork() {
            return getHandle().isInTransaction();
        }

        @Transaction(READ_UNCOMMITTED)
        default boolean transactionWithIsolation() {
            return getHandle().isInTransaction();
        }

        @Transaction(READ_UNCOMMITTED)
        default void nestedTransactionWithSameIsolation() {
            assertTrue(transactionWithIsolation());
        }

        @Transaction(READ_COMMITTED)
        default void nestedTransactionWithDifferentIsolation() {
            transactionWithIsolation();
        }
    }

}

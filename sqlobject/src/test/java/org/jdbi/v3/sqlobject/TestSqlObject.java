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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.jdbi.v3.core.H2DatabaseRule;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.exception.TransactionException;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.sqlobject.customizers.Define;
import org.jdbi.v3.sqlobject.customizers.MaxRows;
import org.jdbi.v3.sqlobject.customizers.RegisterRowMapper;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.subpackage.BrokenDao;
import org.jdbi.v3.sqlobject.subpackage.SomethingDao;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class TestSqlObject
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
        assertThat(c).isEqualTo(new Something(3, "Cora"));
    }

    @Test
    public void testUnimplementedMethod() throws Exception
    {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Method UnimplementedDao.totallyBroken must be default " +
                "or be annotated with a SQL method annotation.");
        handle.attach(UnimplementedDao.class);
    }

    @Test
    public void testRedundantMethodHasDefaultImplementAndAlsoSqlMethodAnnotation() throws Exception
    {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Default method RedundantDao.list has @SqlQuery annotation. " +
                "SQL object methods may be default, or have a SQL method annotation, but not both.");
        handle.attach(RedundantDao.class);
    }

    @Test
    public void testPassThroughMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = handle.attach(SomethingDao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c).isEqualTo(new Something(3, "Cora"));
    }

    @Test(expected = IllegalStateException.class)
    public void testUnimplementedMethodWithDaoInAnotherPackage() throws Exception
    {
        BrokenDao dao = handle.attach(BrokenDao.class);
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
        assertThat(dao.doesTransactionAnnotationWork()).isTrue();
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
        assertThat(dao.findById(1)).isEqualTo(new Something(1, "foo"));

        assertThat(dao.insertTransactional(2, "bar")).isEqualTo(1);
        verify(handle, times(1)).begin();
        assertThat(dao.findById(2)).isEqualTo(new Something(2, "bar"));
    }

    @Test
    public void testRedundantMethodCustomizingAnnotation() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Statement customizing annotations don't work on default methods.");

        handle.attach(RedundantMethodStatementCustomizingAnnotation.class);
    }

    @Test
    public void testRedundantParameterCustomizingAnnotation() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Statement customizing annotations don't work on default methods.");

        handle.attach(RedundantParameterStatementCustomizingAnnotation.class);
    }

    @Test
    public void testRedundantParameterBindingAnnotation() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Binding annotations don't work on default methods.");

        handle.attach(RedundantParameterBindingAnnotation.class);
    }

    @Test
    public void testBooleanReturn() {
        Dao dao = handle.attach(Dao.class);
        assertThat(dao.insert(1, "a")).isTrue();
        assertThat(dao.update(2, "b")).isFalse();
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        boolean insert(@Bind("id") int id, @Bind("name") String name);

        @SqlUpdate("update something set name=:name where id=:id")
        boolean update(int id, String name);


        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        @Transaction
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        Integer insertTransactional(@Bind("id") int id, @Bind("name") String name);

        default Something findByIdHeeHee(int id) {
            return findById(id);
        }

        @Transaction
        default void threeNestedTransactions() {
            twoNestedTransactions();
        }

        @Transaction
        default void twoNestedTransactions() {
            assertThat(doesTransactionAnnotationWork()).isTrue();
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
            assertThat(transactionWithIsolation()).isTrue();
        }

        @Transaction(READ_COMMITTED)
        default void nestedTransactionWithDifferentIsolation() {
            transactionWithIsolation();
        }
    }

    public interface UnimplementedDao extends GetHandle
    {
        void totallyBroken();
    }

    public interface RedundantDao extends GetHandle
    {
        @SqlQuery("select * from something")
        @RegisterRowMapper(SomethingMapper.class)
        default List<Something> list()
        {
            return getHandle().createQuery("select * from something")
                    .map(new SomethingMapper())
                    .list();
        }
    }

    public interface RedundantMethodStatementCustomizingAnnotation extends GetHandle {
        @MaxRows(10)
        default List<String> broken() {
            return emptyList();
        }
    }

    public interface RedundantParameterStatementCustomizingAnnotation extends GetHandle {
        default List<String> broken(@Define int wut) {
            return emptyList();
        }
    }

    public interface RedundantParameterBindingAnnotation extends GetHandle {
        default String broken(@Bind int wat) {
            return "foo";
        }
    }
}

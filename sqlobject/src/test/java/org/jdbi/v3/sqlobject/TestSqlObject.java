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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Something;
import org.jdbi.v3.core.mapper.SomethingMapper;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.core.transaction.TransactionException;
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.customizer.MaxRows;
import org.jdbi.v3.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.v3.sqlobject.statement.SqlBatch;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.subpackage.BrokenDao;
import org.jdbi.v3.sqlobject.subpackage.SomethingDao;
import org.jdbi.v3.sqlobject.transaction.Transaction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import static java.util.Collections.emptyList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.v3.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestSqlObject {
    @Rule
    public H2DatabaseRule dbRule = new H2DatabaseRule().withSomething().withPlugin(new SqlObjectPlugin());

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Handle handle;

    @Before
    public void setUp() {
        handle = dbRule.getSharedHandle();
    }

    @Test
    public void testPassThroughMethod() {
        Dao dao = handle.attach(Dao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c).isEqualTo(new Something(3, "Cora"));
    }

    @Test
    public void testUnimplementedMethod() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Method UnimplementedDao.totallyBroken must be default "
            + "or be annotated with a SQL method annotation.");
        handle.attach(UnimplementedDao.class);
    }

    @Test
    public void testRedundantMethodHasDefaultImplementAndAlsoSqlMethodAnnotation() {
        exception.expect(IllegalStateException.class);
        exception.expectMessage("Default method RedundantDao.list has @SqlQuery annotation. "
            + "SQL object methods may be default, or have a SQL method annotation, but not both.");
        handle.attach(RedundantDao.class);
    }

    @Test
    public void testPassThroughMethodWithDaoInAnotherPackage() {
        SomethingDao dao = handle.attach(SomethingDao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c).isEqualTo(new Something(3, "Cora"));
    }

    @Test
    public void testUnimplementedMethodWithDaoInAnotherPackage() {
        assertThatThrownBy(() -> handle.attach(BrokenDao.class)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testSimpleTransactionsSucceed() {
        SomethingDao dao = dbRule.getJdbi().onDemand(SomethingDao.class);

        dao.insertInSingleTransaction(10, "Linda");
    }

    @Test
    public void testTransactionAnnotationWorksOnInterfaceDefaultMethod() {
        Dao dao = dbRule.getSharedHandle().attach(Dao.class);
        assertThat(dao.doesTransactionAnnotationWork()).isTrue();
    }

    @Test
    public void testNestedTransactionsCollapseIntoSingleTransaction() {
        Handle handle = Mockito.spy(dbRule.getSharedHandle());
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
        Handle handle = Mockito.spy(dbRule.getSharedHandle());
        Dao dao = handle.attach(Dao.class);

        dao.nestedTransactionWithSameIsolation();
        verify(handle, times(1)).begin();
        verify(handle, times(1)).commit();
    }

    @Test
    public void testNestedTransactionWithDifferentIsoltion() {
        Handle handle = Mockito.spy(dbRule.getSharedHandle());
        Dao dao = handle.attach(Dao.class);

        assertThatThrownBy(dao::nestedTransactionWithDifferentIsolation).isInstanceOf(TransactionException.class);
    }

    @Test
    public void testSqlUpdateWithTransaction() {
        Handle handle = Mockito.spy(dbRule.getSharedHandle());
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
        exception.expectMessage("Statement customizing annotations don't work on default methods.");

        handle.attach(RedundantParameterBindingAnnotation.class);
    }

    @Test
    public void testBooleanReturn() {
        Dao dao = handle.attach(Dao.class);
        assertThat(dao.insert(1, "a")).isTrue();
        assertThat(dao.update(2, "b")).isFalse();
    }

    @Test
    public void testSubInterfaceOverridesSuperMethods() {
        SubclassDao dao = handle.attach(SubclassDao.class);
        dao.insert(new Something(1, "foo"));
        assertThat(dao.get(1)).isEqualTo(new Something(1, "foo"));
    }

    @Test
    public void testStaticMethod() {
        handle.attach(StaticDao.class);
        assertThat(StaticDao.staticMethod()).isEqualTo(42);
    }

    @RegisterRowMapper(SomethingMapper.class)
    public interface Dao extends SqlObject {
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

    public interface UnimplementedDao extends SqlObject {
        void totallyBroken();
    }

    public interface RedundantDao extends SqlObject {
        @SqlQuery("select * from something")
        @RegisterRowMapper(SomethingMapper.class)
        default List<Something> list() {
            return getHandle().createQuery("select * from something")
                    .map(new SomethingMapper())
                    .list();
        }
    }

    public interface RedundantMethodStatementCustomizingAnnotation extends SqlObject {
        @MaxRows(10)
        default List<String> broken() {
            return emptyList();
        }
    }

    public interface RedundantParameterStatementCustomizingAnnotation extends SqlObject {
        default List<String> broken(@Define int wut) {
            return emptyList();
        }
    }

    public interface RedundantParameterBindingAnnotation extends SqlObject {
        default String broken(@Bind int wat) {
            return "foo";
        }
    }

    public interface BaseDao<T> {
        void insert(T obj);
        T get(long id);
    }

    public interface SubclassDao extends BaseDao<Something> {
        @Override
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);

        @Override
        @SqlQuery("select * from something where id = :id")
        @RegisterBeanMapper(Something.class)
        Something get(long id);
    }

    public interface StaticDao extends SqlObject {
        static int staticMethod() {
            return 42;
        }
    }

    @Test
    public void genericSuperclassExtendedByExplicitTypedSubclass() {
        ExtendGenericDaoWithExplicitParameter dao = handle.attach(ExtendGenericDaoWithExplicitParameter.class);

        Something alice = new Something(1, "Alice");
        Something bob = new Something(2, "Bob");
        dao.batchInsert(Arrays.asList(alice, bob));

        assertThat(dao.list()).containsExactly(alice, bob);
    }

    @UseClasspathSqlLocator
    public interface GenericDao<T> {
        @SqlBatch
        void batchInsert(@BindBean Collection<T> entities);

        @SqlQuery
        List<T> list();
    }

    @RegisterBeanMapper(Something.class)
    public interface ExtendGenericDaoWithExplicitParameter extends GenericDao<Something> {}
}

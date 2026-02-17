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
package org.jdbi.sqlobject;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jdbi.core.Handle;
import org.jdbi.core.Jdbi;
import org.jdbi.core.Something;
import org.jdbi.core.mapper.SomethingMapper;
import org.jdbi.core.transaction.TransactionException;
import org.jdbi.sqlobject.config.RegisterBeanMapper;
import org.jdbi.sqlobject.config.RegisterRowMapper;
import org.jdbi.sqlobject.customizer.Bind;
import org.jdbi.sqlobject.customizer.BindBean;
import org.jdbi.sqlobject.customizer.Define;
import org.jdbi.sqlobject.customizer.MaxRows;
import org.jdbi.sqlobject.locator.UseClasspathSqlLocator;
import org.jdbi.sqlobject.statement.SqlBatch;
import org.jdbi.sqlobject.statement.SqlQuery;
import org.jdbi.sqlobject.statement.SqlUpdate;
import org.jdbi.sqlobject.subpackage.BrokenDao;
import org.jdbi.sqlobject.subpackage.SomethingDao;
import org.jdbi.sqlobject.transaction.Transaction;
import org.jdbi.testing.junit.JdbiExtension;
import org.jdbi.testing.junit.internal.TestingInitializers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static java.util.Collections.emptyList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.jdbi.core.transaction.TransactionIsolationLevel.READ_COMMITTED;
import static org.jdbi.core.transaction.TransactionIsolationLevel.READ_UNCOMMITTED;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestSqlObject {
    @RegisterExtension
    public JdbiExtension h2Extension = JdbiExtension.h2();

    Connection c;
    Jdbi jdbi;
    Handle handle;

    @BeforeEach
    public void setUp() throws SQLException {
        c = Mockito.mock(Connection.class, Mockito.withSettings()
                .defaultAnswer(AdditionalAnswers.delegatesTo(
                        h2Extension.getSharedHandle().getConnection())));
        jdbi = Jdbi.create(c).installPlugin(new SqlObjectPlugin());
        handle = jdbi.open();
        TestingInitializers.something().initialize(null, handle);
    }

    @AfterEach
    public void close() throws SQLException {
        if (handle != null) {
            handle.close();
        }
        if (c != null) {
            c.close();
        }
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
        UnimplementedDao dao = handle.attach(UnimplementedDao.class);
        assertThatThrownBy(() -> dao.totallyBroken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Method UnimplementedDao.totallyBroken has no registered extension handler!");
    }

    @Test
    public void testRedundantMethodHasDefaultImplementAndAlsoSqlMethodAnnotation() {
        assertThatThrownBy(() -> handle.attach(RedundantDao.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Default method RedundantDao.list has @SqlQuery annotation. Extension type methods may be default, or have a @UseExtensionHandler annotation, but not both.");
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
        // new extension framework no longer fails here.
        BrokenDao dao = handle.attach(BrokenDao.class);

        assertThatThrownBy(() -> dao.totallyBroken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Method BrokenDao.totallyBroken has no registered extension handler!");
    }

    @Test
    public void testSimpleTransactionsSucceed() {
        SomethingDao dao = jdbi.onDemand(SomethingDao.class);

        assertThat(dao.insertInSingleTransaction(10, "Linda")).isOne();
    }

    @Test
    public void testTransactionAnnotationWorksOnInterfaceDefaultMethod() {
        Dao dao = handle.attach(Dao.class);
        assertThat(dao.doesTransactionAnnotationWork()).isTrue();
    }

    @Test
    public void testNestedTransactionsCollapseIntoSingleTransaction() throws SQLException {
        Dao dao = handle.attach(Dao.class);

        dao.threeNestedTransactions();
        verify(c, times(1)).setAutoCommit(false);
        verify(c, times(1)).commit();

        dao.twoNestedTransactions();
        verify(c, times(2)).setAutoCommit(false);
        verify(c, times(2)).commit();
    }

    @Test
    public void testNestedTransactionWithSameIsolation() throws SQLException {
        Dao dao = handle.attach(Dao.class);

        dao.nestedTransactionWithSameIsolation();
        verify(c, times(1)).setAutoCommit(false);
        verify(c, times(1)).commit();
    }

    @Test
    public void testNestedTransactionWithDifferentIsolation() {
        Dao dao = handle.attach(Dao.class);

        assertThatThrownBy(dao::nestedTransactionWithDifferentIsolation).isInstanceOf(TransactionException.class);
    }

    @Test
    public void testSqlUpdateWithTransaction() throws SQLException {
        Dao dao = handle.attach(Dao.class);

        dao.insert(1, "foo");
        verify(c, never()).setAutoCommit(ArgumentMatchers.anyBoolean());
        assertThat(dao.findById(1)).isEqualTo(new Something(1, "foo"));

        assertThat(dao.insertTransactional(2, "bar")).isOne();
        verify(c, times(1)).setAutoCommit(false);
        assertThat(dao.findById(2)).isEqualTo(new Something(2, "bar"));
    }

    @Test
    public void testDefaultMethodCustomizingAnnotation() {
        AnnotatedDefaultMethodDao dao = handle.attach(AnnotatedDefaultMethodDao.class);
        assertThat(dao.annotatedMethod()).isEmpty();
    }

    @Test
    public void testDefaultMethodParameterDefineAnnotation() {
        AnnotatedDefaultMethodDao dao = handle.attach(AnnotatedDefaultMethodDao.class);
        assertThat(dao.annotatedDefineParameter(20)).isEmpty();
    }

    @Test
    public void testDefaultMethodParameterBindAnnotation() {
        AnnotatedDefaultMethodDao dao = handle.attach(AnnotatedDefaultMethodDao.class);
        assertThat(dao.annotatedBindParameter(20)).isEqualTo("foo");
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

    public interface AnnotatedDefaultMethodDao extends SqlObject {
        @MaxRows(10)
        default List<String> annotatedMethod() {
            return emptyList();
        }

        default List<String> annotatedDefineParameter(@Define int wut) {
            return emptyList();
        }

        default String annotatedBindParameter(@Bind int wat) {
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

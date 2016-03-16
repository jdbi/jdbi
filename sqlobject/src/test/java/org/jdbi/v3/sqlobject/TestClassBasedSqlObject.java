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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.exceptions.TransactionException;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.mixins.GetHandle;
import org.jdbi.v3.sqlobject.subpackage.SomethingDao;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestClassBasedSqlObject
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());
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

    @Test(expected = NoSuchMethodError.class)
    public void testUnimplementedMethod() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
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

    @Test(expected = NoSuchMethodError.class)
    public void testUnimplementedMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = handle.attach(SomethingDao.class);
        dao.totallyBroken();
    }

    @Test
    public void testSimpleTransactionsSucceed() throws Exception
    {
        SomethingDao dao = db.getDbi().onDemand(SomethingDao.class);

        dao.insertInSingleTransaction(10, "Linda");
    }

    @Test
    public void testTransactionAnnotationWorksOnInterfaceDefaultMethod() throws Exception
    {
        Dao dao = db.getSharedHandle().attach(Dao.class);
        assertTrue(dao.doesTransactionAnnotationWork());
    }

    /**
     * Currently, nested transactions are not supported. Make sure an appropriate exception is
     * thrown in that case.
     * <p>
     *
     * Side note: H2 does not have a problem with nested transactions - but MySQL has.
     */
    @Test(expected = TransactionException.class)
    public void testNestedTransactionsThrowException()
    {
        SomethingDao dao = db.getDbi().onDemand(SomethingDao.class);
        dao.insertInNestedTransaction(11, "Angelina");
    }

    @RegisterMapper(SomethingMapper.class)
    public interface Dao extends GetHandle
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);

        default Something findByIdHeeHee(int id) {
            return findById(id);
        }

        void totallyBroken();

        @Transaction
        default boolean doesTransactionAnnotationWork() {
            return getHandle().isInTransaction();
        }
    }

}

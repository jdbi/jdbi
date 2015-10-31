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

import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Something;
import org.jdbi.v3.exceptions.TransactionException;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.jdbi.v3.sqlobject.subpackage.SomethingDao;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestClassBasedSqlObject
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        handle = db.getSharedHandle();

    }

    @Test
    public void testPassThroughMethod() throws Exception
    {
        Dao dao = SqlObjectBuilder.attach(handle, Dao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c, equalTo(new Something(3, "Cora")));
    }

    @Test(expected = AbstractMethodError.class)
    public void testUnimplementedMethod() throws Exception
    {
        Dao dao = SqlObjectBuilder.attach(handle, Dao.class);
        dao.totallyBroken();
    }

    @Test
    public void testPassThroughMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = SqlObjectBuilder.attach(handle, SomethingDao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c, equalTo(new Something(3, "Cora")));
    }

    @Test(expected = AbstractMethodError.class)
    public void testUnimplementedMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = SqlObjectBuilder.attach(handle, SomethingDao.class);
        dao.totallyBroken();
    }

    @Test
    public void testSimpleTransactionsSucceed() throws Exception
    {
        final SomethingDao dao = SqlObjectBuilder.onDemand(db.getDbi(), SomethingDao.class);

        dao.insertInSingleTransaction(10, "Linda");
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
        final SomethingDao dao = SqlObjectBuilder.onDemand(db.getDbi(), SomethingDao.class);
        dao.insertInNestedTransaction(11, "Angelina");
    }

    @RegisterMapper(SomethingMapper.class)
    public static abstract class Dao
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public abstract void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select id, name from something where id = :id")
        public abstract Something findById(@Bind("id") int id);

        public Something findByIdHeeHee(int id) {
            return findById(id);
        }

        public abstract void totallyBroken();

    }

}

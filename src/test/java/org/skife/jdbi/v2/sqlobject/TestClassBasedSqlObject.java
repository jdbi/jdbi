package org.skife.jdbi.v2.sqlobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.subpackage.SomethingAgainDao;
import org.skife.jdbi.v2.sqlobject.subpackage.SomethingDao;
import org.skife.jdbi.v2.sqlobject.subpackage.SomethingYetAgainDao;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestClassBasedSqlObject
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:");
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

    }

    @After
    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testPassThroughMethod() throws Exception
    {
        Dao dao = handle.attach(Dao.class);
        dao.insert(3, "Cora");

        Something c = dao.findByIdHeeHee(3);
        assertThat(c, equalTo(new Something(3, "Cora")));
    }

    @Test(expected = AbstractMethodError.class)
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

    @Test
    public void testPassThroughMethodWithDaoInAnotherPackage_Subclass() throws Exception
    {
        SomethingDao dao1 = handle.attach(SomethingDao.class);
        SomethingDao dao2 = handle.attach(SomethingAgainDao.class);
        SomethingDao dao3 = handle.attach(SomethingYetAgainDao.class);

        dao1.insert(3, "Cora");

        Something c = dao1.findById(3);
        assertThat(c, equalTo(new Something(3, "Cora")));

        Something d = dao2.findByName("Cora");
        assertThat(d, equalTo(new Something(3, "Cora")));

        // This implementation actually ends up matching!
        Something dd = dao2.findByNotName("Cora");
        assertThat(dd, equalTo(new Something(3, "Cora")));

        // This method selects something that is not the name given
        Something e = dao3.findByNotName("Delta");
        assertThat(e, equalTo(new Something(3, "Cora")));

        try {
            Something f = dao1.findByNotName("Delta");
            assertThat(f, equalTo(new Something(3, "Cora")));
            fail("Should not succeed for SomethingDao");
        } catch (Throwable th) { }
    }

    @Test(expected = AbstractMethodError.class)
    public void testUnimplementedMethodWithDaoInAnotherPackage() throws Exception
    {
        SomethingDao dao = handle.attach(SomethingDao.class);
        dao.totallyBroken();
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

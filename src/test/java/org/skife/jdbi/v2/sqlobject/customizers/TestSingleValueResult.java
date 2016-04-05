package org.skife.jdbi.v2.sqlobject.customizers;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.*;

import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by anev on 05/04/16.
 */
public class TestSingleValueResult {
    private DBI dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");

        Dao s = handle.attach(Dao.class);
        s.insert(new Something(1, "unique"));
        s.insert(new Something(2, "duplicate"));
        s.insert(new Something(3, "duplicate"));

    }

    @After
    public void tearDown() throws Exception {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testFindUniqueOld() throws Exception {
        Dao dao = handle.attach(Dao.class);

        Something smth = dao.findByName("unique");
        assertEquals(1, smth.getId());
    }

    @Test(expected = MaxRowCountExceedException.class)
    public void testFindDuplicateNew() throws Exception {
        Dao dao = handle.attach(Dao.class);

        dao.findByNameNew("duplicate");
    }

    @Test
    public void testFindUniqueNew() throws Exception {
        Dao dao = handle.attach(Dao.class);

        Something smth = dao.findByNameNew("unique");
        assertEquals(1, smth.getId());
    }

    @Test
    public void testFindUniqueNew2() throws Exception {
        Dao dao = handle.attach(Dao.class);

        Something smth = dao.findByNameNew2("duplicate");
        assertEquals(2, smth.getId());
    }

    @Test
    public void testFindDuplicateOld() throws Exception {
        Dao dao = handle.attach(Dao.class);
        Something smth = dao.findByName("duplicate");
        assertEquals(2, smth.getId());
    }


    interface Dao {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something s);

        @SqlQuery("select id, name from something where name = :name")
        @MaxRowCountConstraint(1)
        Something findByNameNew(@Bind("name") String name);

        @SqlQuery("select id, name from something where name = :name")
        @MaxRowCountConstraint(2)
        Something findByNameNew2(@Bind("name") String name);

        @SqlQuery("select id, name from something where name = :name")
        Something findByName(@Bind("name") String name);

    }

}

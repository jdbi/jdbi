package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.sqlobject.binders.Bind;
import org.skife.jdbi.v2.sqlobject.binders.BindBean;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;
import org.skife.jdbi.v2.util.IntegerMapper;

import java.util.List;
import java.util.Random;

public class TestExamples extends TestCase
{
    private DBI    dbi;
    private Handle handle;

    interface TheBasics
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);

        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") long id);
    }

    @Test
    public void useTheBasics() throws Exception
    {
        TheBasics dao = dbi.onDemand(TheBasics.class);

        dao.insert(new Something(7, "Martin"));

        assertEquals(Integer.valueOf(1), handle.createQuery("select count(*) from something")
                                               .map(IntegerMapper.FIRST)
                                               .first());

        Something martin = dao.findById(7);

        assertEquals("Martin", martin.getName());
    }




    interface SomeBeanBinding
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@BindBean Something something);

        @SqlQuery("select id, name from something where id > :first.id and id < :second.id")
        List<Something> findBetween(@BindBean("first") Something first, @BindBean("second") Something second);
    }

    public void testExerciseSomeBeanBinding() throws Exception
    {
        SomeBeanBinding dao = dbi.onDemand(SomeBeanBinding.class);

        Something martin = new Something(2, "Martin");
        Something david = new Something(3, "David");
        Something tim = new Something(4, "Tim");
        Something eric = new Something(5, "Eric");

        dao.insert(martin);
        dao.insert(david);
        dao.insert(tim);
        dao.insert(eric);

        List<Something> middle = dao.findBetween(martin, eric);

        assertEquals(2, middle.size());
        assertTrue(middle.contains(david));
        assertTrue(middle.contains(tim));
    }


    interface UsesTransactions extends Transactional<UsesTransactions>
    {

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something something);

        @SqlUpdate("update something set name = :name where id = :id")
        int update(@BindBean Something s);

        @SqlQuery("select id, name from something where id = :it")
        Something findById(@Bind int id);
    }

    @Test
    public void exerciseTransactional() throws Exception
    {
        UsesTransactions one = dbi.onDemand(UsesTransactions.class);
        UsesTransactions two = dbi.onDemand(UsesTransactions.class);

        one.insert(new Something(8, "Mike"));

        one.begin();
        one.update(new Something(8, "Michael"));

        assertEquals("Mike", two.findById(8).getName());

        one.commit();

        assertEquals("Michael", two.findById(8).getName());
    }

    @Test
    public void exerciseTransactionalWithCallback() throws Exception
    {
        UsesTransactions dao = dbi.onDemand(UsesTransactions.class);
        dao.insert(new Something(8, "Mike"));

        int rows_updated = dao.inTransaction(new Transaction<Integer, UsesTransactions>()
        {
            public Integer inTransaction(UsesTransactions transactional, TransactionStatus status) throws Exception
            {
                Something current = transactional.findById(8);
                if ("Mike".equals(current.getName())) {
                    return transactional.update(new Something(8, "Michael"));
                }
                else {
                    return 0;
                }
            }
        });

        assertEquals(1, rows_updated);
    }




    @ExternalizedSqlViaStringTemplate3
    static interface UsesLocator extends CloseMe
    {
        @SqlQuery
        public Something findById(@Bind("id") Long id);
    }

//    @StringTemplateSqlSelector("woof.stg")
//    static interface UsesLocator extends CloseMe
//    {
//        @NamedQuery("meow")
//        public Something findById(@Bind("id") Long id);
//    }
//
//    @Test
//    public void testWaffles() throws Exception
//    {
//
//    }





    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        // in MVCC mode h2 doesn't shut down immediately on all connections closed, so need random db name
        ds.setURL("jdbc:h2:mem:test" + new Random().nextInt() +";MVCC=TRUE");

        dbi = new DBI(ds);

        dbi.registerMapper(new SomethingMapper());

        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    public void tearDown() throws Exception
    {
        handle.execute("drop table something");
        handle.close();
    }
}

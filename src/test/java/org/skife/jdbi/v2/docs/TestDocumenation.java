package org.skife.jdbi.v2.docs;

import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Folder2;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.Binder;
import org.skife.jdbi.v2.sqlobject.BinderFactory;
import org.skife.jdbi.v2.sqlobject.BindingAnnotation;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.BatchChunkSize;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringMapper;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.skife.jdbi.v2.ExtraMatchers.equalsOneOf;

public class TestDocumenation
{

    @Before
    public void setUp() throws Exception
    {

    }

    @Test
    public void testFiveMinuteFluentApi() throws Exception
    {
        // using in-memory H2 database via a pooled DataSource
        JdbcConnectionPool ds = JdbcConnectionPool.create("jdbc:h2:mem:test",
                                                          "username",
                                                          "password");
        DBI dbi = new DBI(ds);
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");

        h.execute("insert into something (id, name) values (?, ?)", 1, "Brian");

        String name = h.createQuery("select name from something where id = :id")
            .bind("id", 1)
            .map(StringMapper.FIRST)
            .first();
        assertThat(name, equalTo("Brian"));

        h.close();
        ds.dispose();
    }

    public interface MyDAO
    {
        @SqlUpdate("create table something (id int primary key, name varchar(100))")
        void createSomethingTable();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);

        /**
         * close with no args is used to close the connection
         */
        void close();
    }

    @Test
    public void testFiveMinuteSqlObjectExample() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");

        MyDAO dao = dbi.open(MyDAO.class);

        dao.createSomethingTable();

        dao.insert(2, "Aaron");

        String name = dao.findNameById(2);

        assertThat(name, equalTo("Aaron"));

        dao.close();
    }


    @Test
    public void testObtainHandleViaOpen() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle handle = dbi.open();

        // make sure to close it!
        handle.close();
    }

    @Test
    public void testObtainHandleInCallback() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        dbi.withHandle(new HandleCallback<Void>()
        {
            public Void withHandle(Handle handle) throws Exception
            {
                handle.execute("create table silly (id int)");
                return null;
            }
        });
    }

    @Test
    public void testExecuteSomeStatements() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();

        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (?, ?)", 3, "Patrick");

        List<Map<String, Object>> rs = h.select("select id, name from something");
        assertThat(rs.size(), equalTo(1));

        Map<String, Object> row = rs.get(0);
        assertThat((Integer) row.get("id"), equalTo(3));
        assertThat((String) row.get("name"), equalTo("Patrick"));

        h.close();
    }

    @Test
    public void testFluentUpdate() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");

        h.createStatement("insert into something(id, name) values (:id, :name)")
            .bind("id", 4)
            .bind("name", "Martin")
            .execute();

        h.close();
    }

    @Test
    public void testFold() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (7, 'Mark')");
        h.execute("insert into something (id, name) values (8, 'Tatu')");


        StringBuilder rs = h.createQuery("select name from something order by id")
            .map(StringMapper.FIRST)
            .fold(new StringBuilder(), new Folder2<StringBuilder>()
            {
                public StringBuilder fold(StringBuilder acc, ResultSet rs, StatementContext ctx) throws SQLException
                {
                    acc.append(rs.getString(1)).append(", ");
                    return acc;
                }
            });
        rs.delete(rs.length() - 2, rs.length()); // trim the extra ", "
        assertThat(rs.toString(), equalTo("Mark, Tatu"));
        h.close();
    }


    @Test
    public void testMappingExampleChainedIterator2() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");


        Iterator<String> rs = h.createQuery("select name from something order by id")
            .map(StringMapper.FIRST)
            .iterator();

        assertThat(rs.next(), equalTo("Brian"));
        assertThat(rs.next(), equalTo("Keith"));
        assertThat(rs.hasNext(), equalTo(false));

        h.close();
    }

    @Test
    public void testMappingExampleChainedIterator3() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(100))");
        h.execute("insert into something (id, name) values (1, 'Brian')");
        h.execute("insert into something (id, name) values (2, 'Keith')");

        for (String name : h.createQuery("select name from something order by id").map(StringMapper.FIRST)) {
            assertThat(name, equalsOneOf("Brian", "Keith"));
        }

        h.close();
    }

    @Test
    public void testAttachToObject() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        MyDAO dao = h.attach(MyDAO.class);

        // do stuff with the dao

        h.close();
    }

    @Test
    public void testOnDemandDao() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        MyDAO dao = dbi.onDemand(MyDAO.class);
    }

    public static interface SomeQueries
    {
        @SqlQuery("select name from something where id = :id")
        String findName(@Bind("id") int id);

        @SqlQuery("select name from something where id > :from and id < :to order by id")
        List<String> findNamesBetween(@Bind("from") int from, @Bind("to") int to);

        @SqlQuery("select name from something order by id")
        Iterator<String> findAllNames();
    }

    @Test
    public void testSomeQueriesWorkCorrectly() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();
        h.execute("create table something (id int primary key, name varchar(32))");
        h.prepareBatch("insert into something (id, name) values (:id, :name)")
            .add().bind("id", 1).bind("name", "Brian")
            .next().bind("id", 2).bind("name", "Robert")
            .next().bind("id", 3).bind("name", "Patrick")
            .next().bind("id", 4).bind("name", "Maniax")
            .submit().execute();

        SomeQueries sq = h.attach(SomeQueries.class);
        assertThat(sq.findName(2), equalTo("Robert"));
        assertThat(sq.findNamesBetween(1, 4), equalTo(Arrays.asList("Robert", "Patrick")));

        Iterator<String> names = sq.findAllNames();
        assertThat(names.next(), equalTo("Brian"));
        assertThat(names.next(), equalTo("Robert"));
        assertThat(names.next(), equalTo("Patrick"));
        assertThat(names.next(), equalTo("Maniax"));
        assertThat(names.hasNext(), equalTo(false));
        h.close();
    }

    @RegisterMapper(SomethingMapper.class)
    public static interface AnotherQuery
    {
        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);
    }

    public static interface YetAnotherQuery
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        Something findById(@Bind("id") int id);
    }

    public static interface BatchInserter
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something... somethings);
    }

    @Test
    public void testAnotherCoupleInterfaces() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();

        h.execute("create table something (id int primary key, name varchar(32))");
        h.attach(BatchInserter.class).insert(new Something(1, "Brian"),
                                             new Something(3, "Patrick"),
                                             new Something(2, "Robert"));

        AnotherQuery aq = h.attach(AnotherQuery.class);
        YetAnotherQuery yaq = h.attach(YetAnotherQuery.class);

        assertThat(yaq.findById(3), equalTo(new Something(3, "Patrick")));
        assertThat(aq.findById(2), equalTo(new Something(2, "Robert")));

        h.close();
    }

    public static interface QueryReturningQuery
    {
        @SqlQuery("select name from something where id = :id")
        Query<String> findById(@Bind("id") int id);
    }

    @Test
    public void testFoo() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();

        h.execute("create table something (id int primary key, name varchar(32))");
        h.attach(BatchInserter.class).insert(new Something(1, "Brian"),
                                             new Something(3, "Patrick"),
                                             new Something(2, "Robert"));

        QueryReturningQuery qrq = h.attach(QueryReturningQuery.class);

        Query<String> q = qrq.findById(1);
        q.setMaxFieldSize(100);
        assertThat(q.first(), equalTo("Brian"));

        h.close();
    }

    public static interface Update
    {
        @SqlUpdate("create table something (id integer primary key, name varchar(32))")
        void createSomethingTable();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@Bind("id") int id, @Bind("name") String name);

        @SqlUpdate("update something set name = :name where id = :id")
        int update(@BindBean Something s);
    }

    @Test
    public void testUpdateAPI() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();

        Update u = h.attach(Update.class);
        u.createSomethingTable();
        u.insert(17, "David");
        u.update(new Something(17, "David P."));

        String name = h.createQuery("select name from something where id = 17")
            .map(StringMapper.FIRST)
            .first();
        assertThat(name, equalTo("David P."));

        h.close();
    }

    public static interface BatchExample
    {
        @SqlBatch("insert into something (id, name) values (:id, :first || ' ' || :last)")
        void insertFamily(@Bind("id") List<Integer> ids,
                          @Bind("first") Iterator<String> firstNames,
                          @Bind("last") String lastName);


        @SqlUpdate("create table something(id int primary key, name varchar(32))")
        void createSomethingTable();

        @SqlQuery("select name from something where id = :it")
        String findNameById(@Bind int id);
    }

    @Test
    public void testBatchExample() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:test");
        Handle h = dbi.open();

        BatchExample b = h.attach(BatchExample.class);
        b.createSomethingTable();

        List<Integer> ids = asList(1, 2, 3, 4, 5);
        Iterator<String> first_names = asList("Tip", "Jane", "Brian", "Keith", "Eric").iterator();

        b.insertFamily(ids, first_names, "McCallister");

        assertThat(b.findNameById(1), equalTo("Tip McCallister"));
        assertThat(b.findNameById(2), equalTo("Jane McCallister"));
        assertThat(b.findNameById(3), equalTo("Brian McCallister"));
        assertThat(b.findNameById(4), equalTo("Keith McCallister"));
        assertThat(b.findNameById(5), equalTo("Eric McCallister"));

        h.close();
    }

    public static interface ChunkedBatchExample
    {
        @SqlBatch("insert into something (id, name) values (:id, :first || ' ' || :last)")
        @BatchChunkSize(2)
        void insertFamily(@Bind("id") List<Integer> ids,
                          @Bind("first") Iterator<String> firstNames,
                          @Bind("last") String lastName);

        @SqlUpdate("create table something(id int primary key, name varchar(32))")
        void createSomethingTable();

        @SqlQuery("select name from something where id = :it")
        String findNameById(@Bind int id);
    }

    public static interface BindExamples
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlUpdate("delete from something where name = :it")
        void deleteByName(@Bind String name);
    }

    public static interface BindBeanExample
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something s);

        @SqlUpdate("update something set name = :s.name where id = :s.id")
        void update(@BindBean("s") Something something);
    }

//    @BindingAnnotation(SomethingBinderFactory.class)
//    @Retention(RetentionPolicy.RUNTIME)
//    @Target({ElementType.PARAMETER})
//    public static @interface BindSomething { }
//
//    public static class SomethingBinderFactory implements BinderFactory
//    {
//        public Binder build(Annotation annotation)
//        {
//            return new Binder<BindSomething, Something>()
//            {
//                public void bind(SQLStatement q, BindSomething bind, Something arg)
//                {
//                    q.bind("ident", arg.getId());
//                    q.bind("nom", arg.getName());
//                }
//            };
//        }
//    }

}



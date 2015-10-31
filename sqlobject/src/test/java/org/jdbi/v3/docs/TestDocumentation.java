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
package org.jdbi.v3.docs;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.jdbi.v3.ExtraMatchers.equalsOneOf;
import static org.junit.Assert.assertThat;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.DBI;
import org.jdbi.v3.H2DatabaseRule;
import org.jdbi.v3.Handle;
import org.jdbi.v3.Query;
import org.jdbi.v3.Something;
import org.jdbi.v3.sqlobject.Bind;
import org.jdbi.v3.sqlobject.BindBean;
import org.jdbi.v3.sqlobject.SomethingMapper;
import org.jdbi.v3.sqlobject.SqlBatch;
import org.jdbi.v3.sqlobject.SqlObjectBuilder;
import org.jdbi.v3.sqlobject.SqlQuery;
import org.jdbi.v3.sqlobject.SqlUpdate;
import org.jdbi.v3.sqlobject.customizers.BatchChunkSize;
import org.jdbi.v3.sqlobject.customizers.Mapper;
import org.jdbi.v3.sqlobject.customizers.RegisterMapper;
import org.junit.Rule;
import org.junit.Test;

public class TestDocumentation
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    @Test
    public void testFiveMinuteFluentApi() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.execute("insert into something (id, name) values (?, ?)", 1, "Brian");

            String name = h.createQuery("select name from something where id = :id")
                .bind("id", 1)
                .mapTo(String.class)
                .findOnly();
            assertThat(name, equalTo("Brian"));
        }
    }

    public interface MyDAO extends Closeable
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

    @Test
    public void testFiveMinuteSqlObjectExample() throws Exception
    {
        try (MyDAO dao = db.getDbi().open(MyDAO.class)) {
            dao.insert(2, "Aaron");

            String name = dao.findNameById(2);

            assertThat(name, equalTo("Aaron"));
        }
    }


    @Test
    public void testObtainHandleViaOpen() throws Exception
    {
        try (Handle handle = db.getDbi().open()) { }
    }

    @Test
    public void testObtainHandleInCallback() throws Exception
    {
        DBI dbi = DBI.create("jdbc:h2:mem:" + UUID.randomUUID());
        dbi.useHandle(handle -> handle.execute("create table silly (id int)"));
    }

    @Test
    public void testExecuteSomeStatements() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.execute("insert into something (id, name) values (?, ?)", 3, "Patrick");

            List<Map<String, Object>> rs = h.select("select id, name from something");
            assertThat(rs.size(), equalTo(1));

            Map<String, Object> row = rs.get(0);
            assertThat((Long) row.get("id"), equalTo(3L));
            assertThat((String) row.get("name"), equalTo("Patrick"));
        }
    }

    @Test
    public void testFluentUpdate() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.createStatement("insert into something(id, name) values (:id, :name)")
                .bind("id", 4)
                .bind("name", "Martin")
                .execute();
        }
    }

    @Test
    public void testMappingExampleChainedIterator2() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.execute("insert into something (id, name) values (1, 'Brian')");
            h.execute("insert into something (id, name) values (2, 'Keith')");

            Iterator<String> rs = h.createQuery("select name from something order by id")
                .mapTo(String.class)
                .iterator();

            assertThat(rs.next(), equalTo("Brian"));
            assertThat(rs.next(), equalTo("Keith"));
            assertThat(rs.hasNext(), equalTo(false));
        }
    }

    @Test
    public void testMappingExampleChainedIterator3() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.execute("insert into something (id, name) values (1, 'Brian')");
            h.execute("insert into something (id, name) values (2, 'Keith')");

            for (String name : h.createQuery("select name from something order by id").mapTo(String.class)) {
                assertThat(name, equalsOneOf("Brian", "Keith"));
            }
        }
    }

    @Test
    public void testAttachToObject() throws Exception
    {
        try (Handle h = db.openHandle();
             MyDAO dao = h.attach(MyDAO.class)) {
            dao.insert(1, "test");
        }
    }

    @Test
    public void testOnDemandDao() throws Exception
    {
        try (MyDAO dao = db.getDbi().onDemand(MyDAO.class)) {
            dao.insert(2, "test");
        }
    }

    public interface SomeQueries
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
        try (Handle h = db.openHandle()) {
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
        }
    }

    @RegisterMapper(SomethingMapper.class)
    public interface AnotherQuery
    {
        @SqlQuery("select id, name from something where id = :id")
        Something findById(@Bind("id") int id);
    }

    public interface YetAnotherQuery
    {
        @SqlQuery("select id, name from something where id = :id")
        @Mapper(SomethingMapper.class)
        Something findById(@Bind("id") int id);
    }

    public interface BatchInserter
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something... somethings);
    }

    @Test
    public void testAnotherCoupleInterfaces() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.attach(BatchInserter.class).insert(new Something(1, "Brian"),
                    new Something(3, "Patrick"),
                    new Something(2, "Robert"));

            AnotherQuery aq = h.attach(AnotherQuery.class);
            YetAnotherQuery yaq = h.attach(YetAnotherQuery.class);

            assertThat(yaq.findById(3), equalTo(new Something(3, "Patrick")));
            assertThat(aq.findById(2), equalTo(new Something(2, "Robert")));
        }
    }

    public interface QueryReturningQuery
    {
        @SqlQuery("select name from something where id = :id")
        Query<String> findById(@Bind("id") int id);
    }

    @Test
    public void testFoo() throws Exception
    {
        try (Handle h = db.openHandle()) {
            h.attach(BatchInserter.class).insert(new Something(1, "Brian"),
                                                 new Something(3, "Patrick"),
                                                 new Something(2, "Robert"));

            QueryReturningQuery qrq = h.attach(QueryReturningQuery.class);

            Query<String> q = qrq.findById(1);
            q.setMaxFieldSize(100);
            assertThat(q.findOnly(), equalTo("Brian"));
        }
    }

    public interface Update
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        int insert(@Bind("id") int id, @Bind("name") String name);

        @SqlUpdate("update something set name = :name where id = :id")
        int update(@BindBean Something s);
    }

    @Test
    public void testUpdateAPI() throws Exception
    {
        try (Handle h = db.openHandle()) {
            Update u = h.attach(Update.class);
            u.insert(17, "David");
            u.update(new Something(17, "David P."));

            String name = h.createQuery("select name from something where id = 17")
                .mapTo(String.class)
                .findOnly();
            assertThat(name, equalTo("David P."));
        }
    }

    public interface BatchExample
    {
        @SqlBatch("insert into something (id, name) values (:id, :first || ' ' || :last)")
        void insertFamily(@Bind("id") List<Integer> ids,
                          @Bind("first") Iterator<String> firstNames,
                          @Bind("last") String lastName);


        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

    @Test
    public void testBatchExample() throws Exception
    {
        try (Handle h = db.openHandle()) {
            BatchExample b = h.attach(BatchExample.class);

            List<Integer> ids = asList(1, 2, 3, 4, 5);
            Iterator<String> first_names = asList("Tip", "Jane", "Brian", "Keith", "Eric").iterator();

            b.insertFamily(ids, first_names, "McCallister");

            assertThat(b.findNameById(1), equalTo("Tip McCallister"));
            assertThat(b.findNameById(2), equalTo("Jane McCallister"));
            assertThat(b.findNameById(3), equalTo("Brian McCallister"));
            assertThat(b.findNameById(4), equalTo("Keith McCallister"));
            assertThat(b.findNameById(5), equalTo("Eric McCallister"));
        }
    }

    public interface ChunkedBatchExample
    {
        @SqlBatch("insert into something (id, name) values (:id, :first || ' ' || :last)")
        @BatchChunkSize(2)
        void insertFamily(@Bind("id") List<Integer> ids,
                          @Bind("first") Iterator<String> firstNames,
                          @Bind("last") String lastName);

        @SqlUpdate("create table something(id int primary key, name varchar(32))")
        void createSomethingTable();

        @SqlQuery("select name from something where id = :id")
        String findNameById(@Bind("id") int id);
    }

    public interface BindExamples
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@Bind("id") int id, @Bind("name") String name);

        @SqlUpdate("delete from something where name = :it")
        void deleteByName(@Bind String name);
    }

    public interface BindBeanExample
    {
        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        void insert(@BindBean Something s);

        @SqlUpdate("update something set name = :s.name where id = :s.id")
        void update(@BindBean("s") Something something);
    }
}



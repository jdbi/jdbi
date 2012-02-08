package org.skife.jdbi.v2.docs;

import com.google.common.collect.ImmutableList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.TestContainerFactory;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SomethingMapper;
import org.skife.jdbi.v2.sqlobject.SqlBatch;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestPaging
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:h2:mem:test");

        dbi.setSQLLog(new PrintStreamLog(System.out));
        handle = dbi.open();
        handle.execute("create table something( id integer primary key, name varchar(100) )");
    }

    @After
    public void tearDown() throws Exception
    {
        handle.close();
    }

    @Test
    public void pagingExample() throws Exception
    {
        Sql sql = handle.attach(Sql.class);

        int[] rs = sql.insert(asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13),
                              asList("Ami", "Brian", "Cora", "David", "Eric",
                                     "Fernando", "Greta", "Holly", "Inigo", "Joy",
                                     "Keith", "Lisa", "Molly"));

        assertThat(rs, equalTo(new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1}));

        ImmutableList<Something> page_one = sql.loadPage(-1, 5);
        assertThat(page_one, equalTo(ImmutableList.of(new Something(1, "Ami"),
                                                      new Something(2, "Brian"),
                                                      new Something(3, "Cora"),
                                                      new Something(4, "David"),
                                                      new Something(5, "Eric"))));

        ImmutableList<Something> page_two = sql.loadPage(page_one.get(page_one.size() - 1).getId(), 5);
        assertThat(page_two, equalTo(ImmutableList.of(new Something(6, "Fernando"),
                                                      new Something(7, "Greta"),
                                                      new Something(8, "Holly"),
                                                      new Something(9, "Inigo"),
                                                      new Something(10, "Joy"))));

    }

    @RegisterContainerMapper(TestContainerFactory.ImmutableListContainerFactory.class)
    @RegisterMapper(SomethingMapper.class)
    public static interface Sql
    {
        @SqlBatch("insert into something (id, name) values (:id, :name)")
        public int[] insert(@Bind("id") Iterable<Integer> ids, @Bind("name") Iterable<String> names);

        @SqlQuery("select id, name from something where id > :end_of_last_page order by id limit :size")
        public ImmutableList<Something> loadPage(@Bind("end_of_last_page") int last, @Bind("size") int size);
    }


}

package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.binders.Bind;
import org.skife.jdbi.v2.sqlobject.binders.BindBean;
import org.skife.jdbi.v2.util.StringMapper;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestBatching
{
    private DBI    dbi;
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        dbi = new DBI(ds);
        dbi.registerMapper(new SomethingMapper());
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
    public void testAPIDesign() throws Exception
    {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Something> to_insert = Arrays.asList(new Something(1, "Tom"), new Something(2, "Tatu"));
        int[] counts = b.insertBeans(to_insert);

        assertThat(counts.length, equalTo(2));
        assertThat(counts[0], equalTo(1));
        assertThat(counts[1], equalTo(1));

        assertThat(b.size(), equalTo(2));
    }

    @Test
    public void testBindConstantValue() throws Exception
    {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Integer> ids = Arrays.asList(1,2,3,4,5);

        b.withConstantValue(ids, "Johan");

        assertThat(b.size(), equalTo(5));

        List<String> names = handle.createQuery("select distinct name from something")
                                   .map(StringMapper.FIRST)
                                   .list();
        assertThat(names, equalTo(Arrays.asList("Johan")));
    }

    @Test
    public void testZipping() throws Exception
    {
        UsesBatching b = handle.attach(UsesBatching.class);
        List<Integer> ids = Arrays.asList(1, 2, 3, 4, 5);
        List<String> names = Arrays.asList("David", "Tim", "Mike");

        b.zipArgumentsTogether(ids, names);

        assertThat(b.size(), equalTo(3));

        List<String> ins_names = handle.createQuery("select distinct name from something order by name")
                                   .map(StringMapper.FIRST)
                                   .list();
        assertThat(ins_names, equalTo(Arrays.asList("David", "Mike", "Tim")));
    }

    public static interface UsesBatching
    {
        @Batch("insert into something (id, name) values (:id, :name)")
        public int[] insertBeans(@BindBean Iterable<Something> elements);

        @Batch("insert into something (id, name) values (:id, :name)")
        public int[] withConstantValue(@Bind("id") Iterable<Integer> ids, @Bind("name") String name);

        @Batch("insert into something (id, name) values (:id, :name)")
        public int[] zipArgumentsTogether(@Bind("id") Iterable<Integer> ids, @Bind("name") List<String> name);


        @SqlQuery("select count(*) from something")
        public int size();
    }
}

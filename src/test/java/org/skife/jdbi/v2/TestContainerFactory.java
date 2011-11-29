package org.skife.jdbi.v2;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.DynamicAny.DynAnyOperations;
import org.skife.jdbi.v2.guava.ImmutableListContainerFactory;
import org.skife.jdbi.v2.guava.OptionalContainerFactory;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterContainerMapper;
import org.skife.jdbi.v2.util.StringMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestContainerFactory
{
    private JdbcConnectionPool ds;
    private DBI                dbi;
    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        this.ds = JdbcConnectionPool.create("jdbc:h2:mem:test", "username", "password");
        this.dbi = new DBI(ds);
        this.dbi.registerContainerFactory(new OptionalContainerFactory());
        this.h = dbi.open();
        h.execute("create table something (id int primary key, name varchar)");


    }

    @After
    public void tearDown() throws Exception
    {
        h.close();
        ds.dispose();
    }

    @Test
    public void testExists() throws Exception
    {
        h.execute("insert into something (id, name) values (1, 'Coda')");

        Optional<String> rs = h.createQuery("select name from something where id = :id")
                               .bind("id", 1)
                               .map(StringMapper.FIRST)
                               .first(Optional.class);

        assertThat(rs.isPresent(), equalTo(true));
        assertThat(rs.get(), equalTo("Coda"));
    }

    @Test
    public void testDoesNotExist() throws Exception
    {
        h.execute("insert into something (id, name) values (1, 'Coda')");

        Optional<String> rs = h.createQuery("select name from something where id = :id")
                               .bind("id", 2)
                               .map(StringMapper.FIRST)
                               .first(Optional.class);

        assertThat(rs.isPresent(), equalTo(false));
    }

    @Test
    public void testOnList() throws Exception
    {
        h.registerContainerFactory(new ImmutableListContainerFactory());

        h.execute("insert into something (id, name) values (1, 'Coda')");
        h.execute("insert into something (id, name) values (2, 'Brian')");

        ImmutableList<String> rs = h.createQuery("select name from something order by id")
                               .map(StringMapper.FIRST)
                               .list(ImmutableList.class);

        assertThat(rs, equalTo(ImmutableList.of("Coda", "Brian")));
    }

    @Test
    public void testWithSqlObject() throws Exception
    {
        Dao dao = dbi.onDemand(Dao.class);
        dao.insert(new Something(1, "Coda"));
        dao.insert(new Something(2, "Brian"));

        ImmutableList<String> rs = dao.findAll();
        assertThat(rs, equalTo(ImmutableList.of("Coda", "Brian")));
    }


    @RegisterContainerMapper(ImmutableListContainerFactory.class)
    public static interface Dao
    {
        @SqlQuery("select name from something order by id")
        public ImmutableList<String> findAll();

        @SqlUpdate("insert into something (id, name) values (:id, :name)")
        public void insert(@BindBean Something it);

    }
}

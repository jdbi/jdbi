package org.skife.jdbi.v2.sqlobject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.ExternalizedSqlViaStringTemplate3;
import org.skife.jdbi.v2.util.StringMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestStringTemplate3Locator
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        DBI dbi = new DBI("jdbc:h2:mem:");
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
    public void testBaz() throws Exception
    {
        Wombat wombat = handle.attach(Wombat.class);
        wombat.insert(new Something(7, "Henning"));

        String name = handle.createQuery("select name from something where id = 7")
                            .map(StringMapper.FIRST)
                            .first();

        assertThat(name, equalTo("Henning"));
    }

    @Test
    public void testBam() throws Exception
    {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = handle.attach(Wombat.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @Test
    public void testBap() throws Exception
    {
        handle.execute("insert into something (id, name) values (2, 'Bean')");
        Wombat w = handle.attach(Wombat.class);
        assertThat(w.findNameFor(2), equalTo("Bean"));
    }

    @Test
    public void testDefines() throws Exception
    {
        handle.attach(Wombat.class).weirdInsert("something", "id", "name", 5, "Bouncer");
        String name = handle.createQuery("select name from something where id = 5")
                            .map(StringMapper.FIRST)
                            .first();

        assertThat(name, equalTo("Bouncer"));
    }


    @Test
    public void testBatching() throws Exception
    {
        Wombat roo = handle.attach(Wombat.class);
        roo.insertBunches(new Something(1, "Jeff"), new Something(2, "Brian"));

        assertThat(roo.findById(1L), equalTo(new Something(1, "Jeff")));
        assertThat(roo.findById(2L), equalTo(new Something(2, "Brian")));
    }

    @ExternalizedSqlViaStringTemplate3
    @RegisterMapper(SomethingMapper.class)
    static interface Wombat
    {
        @SqlUpdate
        public void insert(@BindBean Something s);

        @SqlQuery
        public Something findById(@Bind("id") Long id);

        @SqlQuery("select name from something where id = :it")
        String findNameFor(@Bind int id);

        @SqlUpdate
        void weirdInsert(@Define("table") String table,
                         @Define("id_column") String idColumn,
                         @Define("value_column") String valueColumn,
                         @Bind("id") int id,
                         @Bind("value") String name);
        @SqlBatch
        void insertBunches(@BindBean Something... somethings);
    }
}

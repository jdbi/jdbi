package org.skife.jdbi.v2.sqlobject;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Something;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestClasspathStatementLocator {
    private Handle handle;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test");
        DBI dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id int primary key, name varchar(100))");
    }

    @After
    public void tearDown() throws Exception {
        handle.execute("drop table something");
        handle.close();
    }

    @Test
    public void testBam() throws Exception {
        handle.execute("insert into something (id, name) values (6, 'Martin')");

        Something s = handle.attach(Cromulence.class).findById(6L);
        assertThat(s.getName(), equalTo("Martin"));
    }

    @RegisterMapper(SomethingMapper.class)
    static interface Cromulence {
        @SqlQuery
        public Something findById(@Bind("id") Long id);
    }
}

package org.skife.jdbi.v2.sqlobject;

import junit.framework.TestCase;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.ColonPrefixNamedParamStatementRewriter;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestEnumArgument extends TestCase
{
    private Handle handle;

    @Before
    public void setUp() throws Exception
    {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:testenum");
        DBI dbi = new DBI(ds);

        // this is the default, but be explicit for sake of clarity in test
        dbi.setStatementRewriter(new ColonPrefixNamedParamStatementRewriter());
        handle = dbi.open();

        handle.execute("create table something (id identity primary key, name varchar(32))");
    }


    enum NameEnum{
        cemo, brian
    }

    @Test
    public void testEnum() throws Exception
    {
        DAO dao =  handle.attach(DAO.class);
        long cemo_id = dao.insert(NameEnum.cemo);
        long brian_id = dao.insert(NameEnum.brian);

        assertThat(dao.findNameById(cemo_id), equalTo(NameEnum.cemo.name()));

    }

    public static interface DAO
    {
        @SqlUpdate("insert into something (name) values (:it)")
        public long insert(@Bind NameEnum name);

        @SqlQuery("select name from something where id = :it")
        public String findNameById(@Bind long id);
    }
}

package org.skife.jdbi.v2.sqlobject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.sqlobject.mixins.CloseMe;
import org.skife.jdbi.v2.tweak.HandleCallback;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * This test is more or less identical to {@link TestGetGeneratedKeys}
 * except that it's meant to test against an Oracle instance (the only
 * database, to my knowledge, that requires the "columns" attribute on
 * {@link GetGeneratedKeys}.
 * 
 * It has to be @Ignored as we cannot include Oracle here.
 * 
 * To enable:
 * 1. Add the Oracle JDBC driver to the pom.xml.
 * 2. Update the JDBC URL/username/password in the DBI constructor.
 * 3. Remove the @Ignore annotation.
 *
 * @author Jack Leow
 */
@Ignore
public class TestGetGeneratedKeysWithColumns {
    private DBI                dbi;

    @Before
    public void setUp() throws Exception
    {
        dbi = new DBI("jdbc:oracle:thin:@...", "scott", "tiger");
        dbi.withHandle(new HandleCallback<Object>()
        {
            public Object withHandle(Handle handle) throws Exception
            {
                handle.execute("create sequence something_id_seq");
                handle.execute("create table something (id number(38), name varchar(32))");
                return null;
            }
        });
    }

    public static interface DAO extends CloseMe
    {
        @SqlUpdate("insert into something (id, name) values (something_id_seq.nextval, :it)")
        @GetGeneratedKeys(columns = "id")
        public long insert(@Bind String name);

        @SqlQuery("select name from something where id = :it")
        public String findNameById(@Bind long id);
    }

    @Test
    public void testFoo() throws Exception
    {
        DAO dao = dbi.open(DAO.class);

        long brian_id = dao.insert("Brian");
        long keith_id = dao.insert("Keith");
        long larry_id = dao.insert("Larry");

        assertThat(dao.findNameById(brian_id), equalTo("Brian"));
        assertThat(dao.findNameById(keith_id), equalTo("Keith"));
        assertThat(dao.findNameById(larry_id), equalTo("Larry"));

        dao.close();
    }

}
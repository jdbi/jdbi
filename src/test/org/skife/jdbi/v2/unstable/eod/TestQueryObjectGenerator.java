package org.skife.jdbi.v2.unstable.eod;

import junit.framework.TestCase;
import org.skife.jdbi.v2.DBITestCase;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.derby.Tools;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.io.PrintWriter;

/**
 *
 */
public class TestQueryObjectGenerator extends DBITestCase
{
    public void testApiWhichTakesConnection() throws Exception
    {
        Handle h = openHandle();
        MyQueries qo = QueryObjectFactory.createQueryObject(MyQueries.class, h.getConnection());
        assertNotNull(qo);
    }

    public void testApiWhichTakesDatasource() throws Exception
    {
        final Handle h = openHandle();
        MyQueries qo = QueryObjectFactory.createQueryObject(MyQueries.class, Tools.getDataSource());
        assertNotNull(qo);
    }
}

/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.exceptions.ExceptionPolicy;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.tweak.HandleCallback;

import java.sql.Connection;
import java.sql.SQLException;

public class TestDBI extends DBITestCase
{
    public void testDataSourceConstructor() throws Exception
    {
        DBI dbi = new DBI(Tools.getDataSource());
        Handle h = dbi.open();
        assertNotNull(h);
        h.close();
    }

    public void testConnectionFactoryCtor() throws Exception
    {
        DBI dbi = new DBI(new ConnectionFactory()
        {
            public Connection openConnection()
            {
                try
                {
                    return Tools.getConnection();
                }
                catch (SQLException e)
                {
                    throw new UnableToObtainConnectionException(e);
                }
            }
        });
        Handle h = dbi.open();
        assertNotNull(h);
        h.close();
    }

    public void testCorrectExceptionOnSQLException() throws Exception
    {
        DBI dbi = new DBI(new ConnectionFactory()
        {
            public Connection openConnection() throws SQLException
            {
                throw new SQLException();
            }
        });

        try
        {
            dbi.open();
            fail("Should have raised an exception");
        }
        catch (UnableToObtainConnectionException e)
        {
            assertTrue(true);
        }
    }

    public void testStaticHandleOpener() throws Exception
    {
        Handle h = DBI.open(Tools.dataSource);
        assertNotNull(h);
        h.close();
    }

    public void testWithHandle() throws Exception
    {
        DBI dbi = new DBI(Tools.getDataSource());
        String value = dbi.withHandle(new HandleCallback<String>() {
            public String withHandle(Handle handle) throws Exception
            {
                handle.insert("insert into something (id, name) values (1, 'Brian')");
                return handle.createQuery("select name from something where id = 1").map(Something.class).first().getName();
            }
        });
        assertEquals("Brian", value);
    }
}

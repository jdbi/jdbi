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
package org.jdbi.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.jdbi.v3.exceptions.UnableToObtainConnectionException;
import org.jdbi.v3.tweak.ConnectionFactory;
import org.jdbi.v3.tweak.HandleCallback;
import org.junit.Rule;
import org.junit.Test;

public class TestDBI
{
    @Rule
    public MemoryDatabase db = new MemoryDatabase();

    @Test
    public void testDataSourceConstructor() throws Exception
    {
        DBI dbi = new DBI(db.getConnectionString());
        Handle h = dbi.open();
        assertNotNull(h);
        h.close();
    }

    @Test
    public void testConnectionFactoryCtor() throws Exception
    {
        DBI dbi = new DBI(new ConnectionFactory()
        {
            @Override
            public Connection openConnection()
            {
                try
                {
                    return DriverManager.getConnection(db.getConnectionString());
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

    @Test
    public void testCorrectExceptionOnSQLException() throws Exception
    {
        DBI dbi = new DBI(new ConnectionFactory()
        {
            @Override
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

    @Test
    public void testWithHandle() throws Exception
    {
        DBI dbi = new DBI(db.getConnectionString());
        String value = dbi.withHandle(new HandleCallback<String>() {
            @Override
            public String withHandle(Handle handle) throws Exception
            {
                handle.insert("insert into something (id, name) values (1, 'Brian')");
                return handle.createQuery("select name from something where id = 1").map(Something.class).first().getName();
            }
        });
        assertEquals("Brian", value);
    }
}

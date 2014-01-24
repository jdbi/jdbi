/*
 * Copyright (C) 2004 - 2014 Brian McCallister
 *
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
package org.skife.jdbi.v2;

import org.junit.Test;
import org.skife.jdbi.v2.exceptions.UnableToObtainConnectionException;
import org.skife.jdbi.v2.tweak.ConnectionFactory;
import org.skife.jdbi.v2.tweak.HandleCallback;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestDBI extends DBITestCase
{
    @Test
    public void testDataSourceConstructor() throws Exception
    {
        DBI dbi = new DBI(DERBY_HELPER.getDataSource());
        Handle h = dbi.open();
        assertNotNull(h);
        h.close();
    }

    @Test
    public void testConnectionFactoryCtor() throws Exception
    {
        DBI dbi = new DBI(new ConnectionFactory()
        {
            public Connection openConnection()
            {
                try
                {
                    return DERBY_HELPER.getConnection();
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
    public void testStaticHandleOpener() throws Exception
    {
        Handle h = DBI.open(DERBY_HELPER.getDataSource());
        assertNotNull(h);
        h.close();
    }

    @Test
    public void testWithHandle() throws Exception
    {
        DBI dbi = new DBI(DERBY_HELPER.getDataSource());
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

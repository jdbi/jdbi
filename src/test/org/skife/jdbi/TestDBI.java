/* Copyright 2004-2006 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import junit.framework.TestCase;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.skife.jdbi.derby.Tools;
import org.springframework.mock.jndi.SimpleNamingContextBuilder;

import java.sql.Connection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class TestDBI extends TestCase
{

    public void setUp() throws Exception
    {
        Tools.start();
    }

    public void tearDown() throws Exception
    {
        Tools.stop();
    }

    public void testPropertiesCtor() throws Exception
    {
        final DBI dbi = new DBI();
        final Handle h = dbi.open();
        final Connection c = h.getConnection();
        assertNotNull(c);
        assertFalse(c.isClosed());
        h.close();
    }

    public void testJNDILookup() throws Exception
    {
        Tools.start();
        EmbeddedDataSource ds = new EmbeddedDataSource();
        ds.setDatabaseName("testing");

        SimpleNamingContextBuilder.emptyActivatedContextBuilder().bind("java:/ds/testing", ds);

        final DBI dbi = new DBI("java:/ds/testing");
        assertNotNull(dbi);
        final Handle h = dbi.open();
        assertTrue(h.isOpen());
        h.close();
    }

    public void testHandleCallback() throws Exception
    {
        Tools.dropAndCreateSomething();

        DBI.open(Tools.CONN_STRING, new HandleCallback()
        {
            public void withHandle(Handle handle) throws Exception
            {
                handle.execute("insert into something (id, name) values (1, 'bob')");
                final Collection results = handle.query("select * from something");
                final Iterator i = results.iterator();
                assertTrue(i.hasNext());
                final Map row = (Map) i.next();
                assertEquals(new Integer(1), row.get("id"));
            }
        });
    }
}

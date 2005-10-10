/* Copyright 2004-2005 Brian McCallister
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
package org.skife.jdbi.unstable.theory;

import junit.framework.TestCase;
import org.skife.jdbi.DBI;
import org.skife.jdbi.Handle;
import org.skife.jdbi.Something;
import org.skife.jdbi.derby.Tools;

import java.util.Iterator;
import java.util.Map;

public class TestTheory extends TestCase
{
    private Handle handle;
    private Database database;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        handle = DBI.open(Tools.CONN_STRING);
        database = new Database(Tools.CONN_STRING);
    }

    public void tearDown() throws Exception
    {
        if (handle.isOpen()) handle.close();
        Tools.stop();
    }

    public void testApiStuff() throws Exception
    {
        handle.execute("insert into something (id, name) values (:id, :name)", new Something(1, "brian"));

        Relation something = database.getRelation("something");
        Iterator i = something.iterator();
        Map row = (Map) i.next();
        assertEquals(new Integer(1), row.get("id"));
    }
}

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
package org.skife.jdbi;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

import java.util.Iterator;
import java.util.Map;
import java.util.ListIterator;

public class TestQueryAPI extends TestCase
{
    private Handle handle;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        handle = DBI.open(Tools.CONN_STRING);
    }

    public void tearDown() throws Exception
    {
        handle.close();
    }

    public void testQueryApiLooks() throws Exception
    {
        Query q = handle.createQuery("select * from something");
        assertNotNull(q);
    }

    public void testResultSetAdvancesOnHasNext() throws Exception
    {
        handle.execute("insert into something (id, name) values (:id, :name)",
                       new Something(1, "one"));

        Iterator i = handle.createQuery("select * from something").iterator();
        assertTrue(i.hasNext());
        // result set should have already advanced
        Map row = (Map) i.next();
        assertEquals(new Integer(1), row.get("id"));
        assertFalse(i.hasNext());
        handle.close(i);
    }

    public void testIteratorQuery2() throws Exception
    {
        handle.execute("insert into something (id, name) values (:id, :name)",
                       new Something(1, "one"));

        Iterator i = handle.createQuery("select * from something").iterator();
        Map row = (Map) i.next();
        // result set should advance on the next
        assertEquals(new Integer(1), row.get("id"));
        handle.close(i);
    }

    public void testListIterator() throws Exception
    {
        handle.prepareBatch("insert into something (id, name) values (:id, :name)")
                .add(new Something(1, "one"))
                .add(new Something(2, "two"))
                .add(new Something(3, "three"))
                .execute();


        ListIterator i = handle.createQuery("select * from something order by id").listIterator();
//        Map row = (Map) i.next();
//        // result set should advance on the next
//        assertEquals(new Integer(1), row.get("id"));
        handle.close(i);
    }

}

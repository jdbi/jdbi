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

import java.util.Collection;
import java.util.Map;

public class TestBeanMappedParams extends TestCase
{
    private Handle h;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        h = DBI.open(Tools.CONN_STRING);
    }

    public void tearDown() throws Exception
    {
        Tools.stop();
        if (h.isOpen()) h.close();
    }

    public void testBeanNaming() throws Exception
    {
        h.execute("insert into something (id, name) values (:id, :name)", new Something(1, "one"));
        assertEquals(1, h.query("select * from something").size());
    }

    public void testBeanMapQuery() throws Exception
    {
        final Something thing = new Something(1, "one");
        h.execute("insert into something (id, name) values (:id, :name)", thing);
        final Collection results = h.query("select name from something where id = :id", thing);
        assertEquals(1, results.size());
        final Map row = (Map) results.iterator().next();
        assertEquals("one", row.get("name"));
    }
}

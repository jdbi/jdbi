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
package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;

import java.util.List;
import java.util.Map;

public class TestQueries extends TestCase
{
    private BasicHandle h;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        h = new BasicHandle(Tools.getConnection());
    }

    public void tearDown() throws Exception
    {
        if (h != null) h.close();
        Tools.stop();
    }

    public void testStatement() throws Exception
    {
        int rows = h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        assertEquals(1, rows);
    }

    public void testCreateQueryObject() throws Exception
    {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        List<Map<String, Object>> results = h.createQuery("select * from something order by id").list();
        assertEquals(2, results.size());
        Map<String, Object> first_row = results.get(0);
        assertEquals("eric", first_row.get("name"));
    }

    public void testMappedQueryObject() throws Exception
    {
        h.createStatement("insert into something (id, name) values (1, 'eric')").execute();
        h.createStatement("insert into something (id, name) values (2, 'brian')").execute();

        Query<Something> query = h.createQuery("select * from something order by id").map(Something.class);

        List<Something> r = query.list();
        Something eric = r.get(0);
        assertEquals("eric", eric.getName());
        assertEquals(1, eric.getId());
    }
}
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
import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.unstable.decorator.HandleDecorator;
import org.skife.jdbi.unstable.decorator.BaseHandleDecorator;

import java.util.Map;

public class TestDecorators extends TestCase
{

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
    }

    public void tearDown() throws Exception
    {
        Tools.stop();
    }

    public void testAutoConfigured() throws Exception
    {
        final DBI dbi = new DBI();
        final Handle h = dbi.open();
        h.execute("insert into something (id, name) values (1, 'one')");
        final Map row = h.first("select * from something");
        assertEquals("hello", row.get("wombat"));
        h.close();
    }

    public void testHandleDecorator() throws Exception
    {
        DBI dbi = new DBI(Tools.CONN_STRING);

        dbi.setHandleDecorator(new MyHandleDecorator());

        final Handle h = dbi.open();
        h.execute("insert into something (id, name) values (:id, :name)", new Something(1, "one"));
        final Map first = h.first("select * from something");
        assertEquals("hello", first.get("wombat"));
        assertEquals("one", first.get("name"));
        h.close();
    }
}

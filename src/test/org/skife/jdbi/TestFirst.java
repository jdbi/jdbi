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

import org.skife.jdbi.derby.Tools;

import java.util.Map;

import junit.framework.TestCase;

public class TestFirst extends TestCase
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
        Tools.stop();
    }

    public void testPlain() throws Exception
    {
        handle.execute("insert into something (id, name) values (1, 'one')");
        Map row = handle.first("select name from something where id = 1");
        assertEquals("one", row.get("name"));
    }
}

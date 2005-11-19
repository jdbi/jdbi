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

import java.util.List;

public class TestNamedQueries extends TestCase
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
        h.close();
        Tools.stop();
    }

    public void testNamedQueryWithComments() throws Exception
    {
        h.execute("insert-with-comment", new Something(1, "one"));
        List r = h.query("all-something");
        assertEquals(1, r.size());
    }
}

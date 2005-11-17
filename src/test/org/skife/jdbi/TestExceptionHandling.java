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

public class TestExceptionHandling extends TestCase
{
    private Handle handle;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        this.handle = DBI.open(Tools.CONN_STRING);
    }

    public void tearDown() throws Exception
    {
        Tools.stop();
    }

    public void testSomething() throws Exception
    {
        try
        {
            handle.query("select wombats from something");
            fail("should throw an exception");
        }
        catch (DBIException e)
        {
            assertTrue( e.getMessage().startsWith("Column 'WOMBATS' is not in any table in the FROM list") );
        }
    }
}

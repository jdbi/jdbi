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

import java.util.Map;
import java.math.BigDecimal;

public class TestGlobalParameters extends TestCase
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

    public void testParamOnBareFirst() throws Exception
    {
        handle.getGlobalParameters().put("name", "Robert");
        handle.execute("insert into something (id, name) values (:id, :name)", new Something(1, "Robert"));

        Map robbie = handle.first("select * from something where name = :name");
        assertNotNull(robbie);
        assertEquals(new Integer("1"), robbie.get("id"));
    }

    public void testParamOnBareQuery() throws Exception
    {
        handle.getGlobalParameters().put("name", "Robert");
        handle.execute("insert into something (id, name) values (:id, :name)", new Something(1, "Robert"));

        Map robbie = (Map) handle.query("select * from something where name = :name").get(0);
        assertNotNull(robbie);
        assertEquals(new Integer("1"), robbie.get("id"));
    }

    public void testSetParamOnBareExecute() throws Exception
    {
        handle.getGlobalParameters().put("name", "Robert");
        handle.execute("insert into something (id, name) values (:id, :name)", Args.with("id", new Integer(1)));

        Map robbie = handle.first("select * from something where name = :name");
        assertNotNull(robbie);
        assertEquals(new Integer("1"), robbie.get("id"));
    }

    public void testGlobalsOnDBIPassThrough() throws Exception
    {
        handle.close();
        DBI dbi = new DBI(Tools.CONN_STRING);
        dbi.getGlobalParameters().put("name", "Robert");
        handle = dbi.open();

        assertTrue(handle.getGlobalParameters().containsKey("name"));
    }

    public void testHandleGlobalsDoNotPassUp() throws Exception
    {
        handle.close();
        DBI dbi = new DBI(Tools.CONN_STRING);
        handle = dbi.open();

        handle.getGlobalParameters().put("name", "Robert");
        assertFalse(dbi.getGlobalParameters().containsKey("name"));
    }
}

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
import org.skife.jdbi.ConnectionHandle;
import org.skife.jdbi.NamedStatementRepository;

import java.util.Map;

public class TestGlobalRepository extends TestCase
{
    private Handle handle;
    private NamedStatementRepository repo;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        repo = new NamedStatementRepository();
        handle = new ConnectionHandle(Tools.getConnection(), repo);
    }

    public void tearDown() throws Exception
    {
        handle.close();
        Tools.stop();
    }

    public void testNamedQueriesGoToRep() throws Exception
    {
        handle.name("query", "select id, name from something");
        assertTrue(repo.contains("query"));
    }

    public void testAdHocDontGoToRep() throws Exception
    {
        handle.execute("select id, name from something");
        assertFalse(repo.contains("select id, name from something"));
    }

    public void testAdHocLoadedQueriesInRep() throws Exception
    {
        handle.execute("all-something");
        assertTrue(repo.contains("all-something"));
    }

    public void testExplicitLoadedQueriesInRep() throws Exception
    {
        handle.load("all-something");
        assertTrue(repo.contains("all-something"));
    }

    public void testDBIInstanceUsesSameRepo() throws Exception
    {
        final DBI dbi = new DBI(Tools.CONN_STRING);
        final Handle one = dbi.open();
        final Handle two = dbi.open();

        one.name("one", "select * from something");
        two.name("two", "select id, name from something");

        one.close();
        two.close();

        final Map statements = dbi.getNamedStatements();
        assertTrue(statements.containsKey("one"));
        assertTrue(statements.containsKey("two"));
    }
}

/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.derby;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.Statement;

public class TestDerbyStuff extends TestCase
{
    public void testNoExceptionOnCreationAndDeletion() throws Exception
    {
        try
        {
            Tools.start();
            Tools.stop();
        }
        catch (Exception e)
        {
            fail("Unable to create and delete test directory: " + e.getMessage());
        }
    }

    public void testCreateSchema() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
        final Connection conn = Tools.getConnection();

        final Statement stmt = conn.createStatement();
        assertTrue(stmt.execute("select count(*) from something"));

        stmt.close();
        conn.close();
        Tools.stop();
    }
}

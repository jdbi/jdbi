/*
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
package org.jdbi.v3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class TestPositionalParameterBinding
{
    @Rule
    public H2DatabaseRule db = new H2DatabaseRule();

    private Handle h;

    @Before
    public void setUp() throws Exception
    {
        h = db.openHandle();
    }

    @Test
    public void testSetPositionalString() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric = h.createQuery("select * from something where name = ?")
                .bind(0, "eric")
                .map(Something.class)
                .list()
                .get(0);
        assertEquals(1, eric.getId());
    }

    @Test
    public void testSetPositionalInteger() throws Exception
    {
        h.insert("insert into something (id, name) values (1, 'eric')");
        h.insert("insert into something (id, name) values (2, 'brian')");

        Something eric = h.createQuery("select * from something where id = ?")
                .bind(0, 1)
                .map(Something.class)
                .list().get(0);
        assertEquals(1, eric.getId());
    }

    @Test
    public void testBehaviorOnBadBinding1() throws Exception
    {
        Query<Something> q = h.createQuery("select * from something where id = ? and name = ?")
                .bind(0, 1)
                .map(Something.class);

        try
        {
            q.list();
            fail("should have thrown exception");
        }
        catch (UnableToExecuteStatementException e)
        {
            assertTrue("Execution goes through here", true);
        }
        catch (Exception e)
        {
            fail("Threw an incorrect exception type " + e.getMessage());
        }
    }

    @Test
    public void testBehaviorOnBadBinding2() throws Exception
    {
        Query<Something> q = h.createQuery("select * from something where id = ?")
                .bind(1, 1)
                .bind(2, "Hi")
                .map(Something.class);

        try
        {
            q.list();
            fail("should have thrown exception");
        }
        catch (UnableToExecuteStatementException e)
        {
            assertTrue("Execution goes through here", true);
        }
        catch (Exception e)
        {
            fail("Threw an incorrect exception type");
        }
    }

    @Test
    public void testInsertParamBinding() throws Exception
    {
        int count = h.createStatement("insert into something (id, name) values (?, 'eric')")
                .bind(0, 1)
                .execute();

        assertEquals(1, count);
    }

    @Test
    public void testPositionalConvenienceInsert() throws Exception
    {
        int count = h.insert("insert into something (id, name) values (?, ?)", 1, "eric");

        assertEquals(1, count);
    }
}

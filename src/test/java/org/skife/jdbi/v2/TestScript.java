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
package org.skife.jdbi.v2;

import org.junit.Test;
import org.skife.jdbi.v2.exceptions.StatementException;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 *
 */
public class TestScript extends DBITestCase
{
    @Test
    public void testScriptStuff() throws Exception
    {
        Handle h = openHandle();
        Script s = h.createScript("default-data");
        s.execute();

        assertEquals(2, h.select("select * from something").size());
    }

    @Test
    public void testScriptWithComments() throws Exception{
        BasicHandle h = openHandle();
        Script script = h.createScript("insert-script-with-comments");
        script.execute();

        assertEquals(3, h.select("select * from something").size());
    }

    @Test
    public void testScriptWithStringSemicolon() throws Exception {
        BasicHandle h = openHandle();
        Script script = h.createScript("insert-with-string-semicolons");
        script.execute();

        assertEquals(3, h.select("select * from something").size());
    }

    @Test
    public void testFuzzyScript() throws Exception {
        BasicHandle h = openHandle();
        Script script = h.createScript("fuzzy-script");
        script.executeAsSeparateStatements();

        List<Map<String, Object>> rows = h.select("select * from something order by id");
        assertEquals(4, rows.size());
        assertEquals(rows.get(0).get("id"), (Integer) 1);
        assertEquals(rows.get(0).get("name"), "eric");
        assertEquals(rows.get(1).get("id"), (Integer) 2);
        assertEquals(rows.get(1).get("name"), "sally;ann");
        assertEquals(rows.get(2).get("id"), (Integer) 3);
        assertEquals(rows.get(2).get("name"), "bob");
        assertEquals(rows.get(3).get("id"), (Integer) 12);
        assertEquals(rows.get(3).get("name"), "sally;ann;junior");
    }

    @Test
    public void testScriptAsSetOfSeparateStatements() throws Exception {
        try {
            BasicHandle h = openHandle();
            Script script = h.createScript("malformed-sql-script");
            script.executeAsSeparateStatements();
            fail("Should fail because the script is malformed");
        } catch (StatementException e) {
            StatementContext context = e.getStatementContext();
            assertEquals(context.getRawSql().trim(), "insert into something(id, name) values (2, eric)");
        }
    }
}

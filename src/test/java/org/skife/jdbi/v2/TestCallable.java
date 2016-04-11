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

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestCallable extends DBITestCase
{
    private BasicHandle h;

    @Override
    public void doSetUp() throws Exception {
        h = openHandle();
        try {
            h.execute("drop function to_degrees");
            h.execute("drop procedure test_procedure");
        }
        catch (Exception e) {
            // okay if not present
        }

        h.execute("CREATE FUNCTION TO_DEGREES(RADIANS DOUBLE) RETURNS DOUBLE\n" +
                  "PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA\n" +
                  "EXTERNAL NAME 'java.lang.Math.toDegrees'");
        h.execute("CREATE PROCEDURE TEST_PROCEDURE(in in_param varchar(20), out out_param varchar(20))\n" +
                "PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA\n" +
                "EXTERNAL NAME 'org.skife.jdbi.v2.TestCallable.testProcedure'");

    }

    @Override
    public void doTearDown() throws Exception {
        try {
            h.execute("drop function to_degrees");
            h.execute("drop procedure test_procedure");
        }
        catch (Exception e) {
            // okay if not present
        }

        if (h != null) h.close();
    }

    @Test
    public void testStatement() throws Exception {
        OutParameters ret = h.createCall("? = CALL TO_DEGREES(?)")
                .registerOutParameter(0, Types.DOUBLE)
                .bind(1, 100.0d)
                .invoke();

        // JDBI oddity : register or bind is 0-indexed, which JDBC is 1-indexed.
        Double expected = Math.toDegrees(100.0d);
        assertEquals(expected, ret.getDouble(1));
        assertEquals(expected.longValue(), ret.getLong(1).longValue());
        assertEquals(expected.shortValue(), ret.getShort(1).shortValue());
        assertEquals(expected.intValue(), ret.getInt(1).intValue());
        assertEquals(expected.floatValue(), ret.getFloat(1), 0.001);

        try {
            ret.getDate(1);
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            //e.printStackTrace();
        }

        try {
            ret.getDate(2);
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            //e.printStackTrace();
        }

    }

    @Test
    public void testStatementWithNamedParam() throws Exception {
        OutParameters ret = h.createCall(":x = CALL TO_DEGREES(:y)")
                .registerOutParameter("x", Types.DOUBLE)
                .bind("y", 100.0d)
                .invoke();

        Double expected = Math.toDegrees(100.0d);
        assertEquals(expected, ret.getDouble("x"));
        assertEquals(expected.longValue(), ret.getLong("x").longValue());
        assertEquals(expected.shortValue(), ret.getShort("x").shortValue());
        assertEquals(expected.intValue(), ret.getInt("x").intValue());
        assertEquals(expected.floatValue(), ret.getFloat("x"), 0.001);

        try {
            ret.getDate("x");
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            //e.printStackTrace();
        }

        try {
            ret.getDate("y");
            fail("didn't throw exception !");
        }
        catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testWithNullReturn() throws Exception {
        OutParameters ret = h.createCall("CALL TEST_PROCEDURE(?, ?)")
                .bind(0, (String)null)
                .registerOutParameter(1, Types.VARCHAR)
                .invoke();

        // JDBI oddity : register or bind is 0-indexed, which JDBC is 1-indexed.
        String out = ret.getString(2);
        assertEquals(out, null);
    }

    @Test
    public void testWithNullReturnWithNamedParam() throws Exception {
        OutParameters ret = h.createCall("CALL TEST_PROCEDURE(:x, :y)")
                .bind("x", (String)null)
                .registerOutParameter("y", Types.VARCHAR)
                .invoke();

        String out = ret.getString("y");
        assertEquals(out, null);
    }

    public static void testProcedure(String in, String[] out) {
        out = new String[1];
        out[0] = in;
    }
}

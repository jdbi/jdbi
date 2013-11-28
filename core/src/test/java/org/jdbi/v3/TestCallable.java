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
package org.jdbi.v3;

import org.jdbi.derby.Tools;
import org.jdbi.v3.BasicHandle;
import org.jdbi.v3.OutParameters;

import java.sql.Types;

public class TestCallable extends DBITestCase
{
    private BasicHandle h;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        h = openHandle();
        try {
            h.execute("drop function to_degrees");
        }
        catch (Exception e) {
            // okay if not present
        }

        h.execute("CREATE FUNCTION TO_DEGREES(RADIANS DOUBLE) RETURNS DOUBLE\n" +
                  "PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA\n" +
                  "EXTERNAL NAME 'java.lang.Math.toDegrees'");
    }

    @Override
    public void tearDown() throws Exception {
        try {
            h.execute("drop function to_degrees");
        }
        catch (Exception e) {
            // okay if not present
        }

        if (h != null) h.close();
        Tools.stop();
    }

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
        assertEquals(expected.floatValue(), ret.getFloat(1));

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
        assertEquals(expected.floatValue(), ret.getFloat("x"));

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

}

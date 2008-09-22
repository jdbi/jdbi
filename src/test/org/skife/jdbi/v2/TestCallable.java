package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;

import java.sql.Types;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.Map;

public class TestCallable extends DBITestCase
{
    private BasicHandle h;

    public void setUp() throws Exception
    {
        super.setUp();
        h = openHandle();
	    try {
		    h.execute("drop function to_degrees");
	    }
	    catch (Exception e) {
	    }
	    h.execute("CREATE FUNCTION TO_DEGREES(RADIANS DOUBLE) RETURNS DOUBLE\n" +
			    "PARAMETER STYLE JAVA NO SQL LANGUAGE JAVA\n" +
			    "EXTERNAL NAME 'java.lang.Math.toDegrees'");
    }

    public void tearDown() throws Exception
    {
	    try {
		    h.execute("drop function to_degrees");
	    }
	    catch (Exception e) {
	    }
        if (h != null) h.close();
        Tools.stop();
    }

    public void testStatement() throws Exception
    {
	    Call.OutParameters ret = h.createCall("? = CALL TO_DEGREES(?)")
			    .registerOutParameter(0, Types.DOUBLE)
			    .bind(1, 100.0d)
			    .invoke();

	    // JDBI oddity : register or bind is 0-indexed, which JDBC is 1-indexed.
	    Double expected = Math.toDegrees(100.0d);
	    assertEquals(expected, ret.getDouble(1));
	    assertEquals(expected.longValue(), ret.getLong(1).longValue());
	    assertEquals(expected.shortValue(), ret.getShort(1).shortValue());
	    assertEquals(expected.intValue(), ret.getInt(1).intValue());
	    assertEquals(expected.floatValue(), ret.getFloat(1).floatValue());
    }

	public void testStatementWithNamedParam() throws Exception
	{
		Call.OutParameters ret = h.createCall(":x = CALL TO_DEGREES(:y)")
				.registerOutParameter("x", Types.DOUBLE)
				.bind("y", 100.0d)
				.invoke();

		Double expected = Math.toDegrees(100.0d);
		assertEquals(expected, ret.getDouble("x"));
		assertEquals(expected.longValue(), ret.getLong("x").longValue());
		assertEquals(expected.shortValue(), ret.getShort("x").shortValue());
		assertEquals(expected.intValue(), ret.getInt("x").intValue());
		assertEquals(expected.floatValue(), ret.getFloat("x").floatValue());
	}

}

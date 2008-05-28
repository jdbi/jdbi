package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;

import java.sql.Types;
import java.sql.CallableStatement;
import java.sql.SQLException;

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
        Double i = h.createCall("? = CALL TO_DEGREES(?)", new CallableStatementMapper<Double>(){
	        public Double map(CallableStatement call) throws SQLException
	        {
		        return call.getDouble(1);
	        }
        })
		.registerOutParameter(0, Types.VARCHAR)
		.bind(1, 100.0d)
		.invoke();

	    assertEquals(Math.toDegrees(100.0d), i);
    }

	public void testStatementWithNamedParam() throws Exception
	{
	    Double i = h.createCall(":x = CALL TO_DEGREES(:y)", new CallableStatementMapper<Double>(){
		    public Double map(CallableStatement call) throws SQLException
		    {
			    return call.getDouble(1);
		    }
	    })
		.registerOutParameter("x", Types.VARCHAR)
		.bind("y", 100.0d)
		.invoke();

		assertEquals(Math.toDegrees(100.0d), i);
	}

}

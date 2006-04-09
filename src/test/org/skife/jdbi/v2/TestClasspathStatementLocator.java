package org.skife.jdbi.v2;

import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;

/**
 * 
 */
public class TestClasspathStatementLocator extends DBITestCase
{
    public void testLocateNamedWithoutSuffix() throws Exception
    {
        Handle h = openHandle();
        h.createStatement("insert-keith").execute();
        assertEquals(1, h.select("select name from something").size());
    }

    public void testLocateNamedWithSuffix() throws Exception
    {
        Handle h = openHandle();
        h.insert("insert-keith.sql");
        assertEquals(1, h.select("select name from something").size());
    }

    public void testCommentsInExternalSql() throws Exception
    {
        Handle h = openHandle();
        h.insert("insert-eric-with-comments");
        assertEquals(1, h.select("select name from something").size());
    }

    public void testNamedPositionalNamedParamsInPrepared() throws Exception
    {
        Handle h = openHandle();
        h.insert("insert-id-name", 3, "Tip");
        assertEquals(1, h.select("select name from something").size());
    }

    public void testNamedParamsInExternal() throws Exception
    {
        Handle h = openHandle();
        h.createStatement("insert-id-name").bind("id", 1).bind("name", "Tip").execute();
        assertEquals(1, h.select("select name from something").size());
    }

    public void testTriesToParseNameIfNothingFound() throws Exception
    {
        Handle h = openHandle();
        try
        {
            h.insert("this-does-not-exist.sql");
            fail("Should have raised an exception");
        }
        catch (UnableToCreateStatementException e)
        {
            assertTrue(true);
        }
    }
}

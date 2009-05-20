/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

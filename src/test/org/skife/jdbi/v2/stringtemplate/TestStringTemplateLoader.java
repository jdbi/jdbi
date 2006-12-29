/* Copyright 2004-2006 Brian McCallister
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

package org.skife.jdbi.v2.stringtemplate;

import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.skife.jdbi.v2.DBITestCase;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.StatementLocator;
import org.skife.jdbi.v2.util.StringMapper;

/**
 *
 */
public class TestStringTemplateLoader extends DBITestCase
{
    public void setUp() throws Exception
    {
        super.setUp();
    }

    protected StatementLocator getStatementLocator()
    {
        final ClasspathGroupLoader loader = new ClasspathGroupLoader(AngleBracketTemplateLexer.class,
                                                                     "org/skife/jdbi/v2/unstable/stringtemplate");
        return new StringTemplateStatementLocator(loader);
    }

    public void testSimpleTemplate() throws Exception
    {
        final Handle h = openHandle();
        final int count = h.insert("tests:insert_one");
        assertEquals(1, count);
    }

    public void testParameterizedInsert() throws Exception
    {
        final Handle h = openHandle();
        final int count = h.createStatement("tests:parameterized_insert")
                .define("table", "something")
                .define("column_one", "id")
                .define("column_two", "name")
                .bind("column_one", 7)
                .bind("column_two", "Rebecca")
                .execute();
        assertEquals(1, count);

        final String name = h.createQuery("select name from something where id = 7")
                .map(StringMapper.FIRST)
                .first();
        assertEquals(name, "Rebecca");
    }

    public void testExtraDefinesDontBreakThings() throws Exception
    {
        final Handle h = openHandle();
        final int count = h.createStatement("tests:insert_one")
                .define("name", "Nicole")
                .execute();
        assertEquals(1, count);
    }
}

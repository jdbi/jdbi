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
package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.v2.tweak.RewrittenStatement;

/**
 *
 */
public class TestColonStatementRewriter extends TestCase
{
    private ColonPrefixNamedParamStatementRewriter rw;

    public void setUp() throws Exception
    {
        this.rw = new ColonPrefixNamedParamStatementRewriter();
    }


    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from something\n where id = :id", new Binding());
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rw.rewrite(":boo ':nope' _%&^& *@ :id", new Binding());
        assertEquals("? ':nope' _%&^& *@ ?", rws.getSql());
    }

    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rw.rewrite(":bo0 ':nope' _%&^& *@ :id", new Binding());
        assertEquals("? ':nope' _%&^& *@ ?", rws.getSql());
    }
}

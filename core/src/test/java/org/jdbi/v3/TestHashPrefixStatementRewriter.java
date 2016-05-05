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

import org.jdbi.v3.exceptions.UnableToCreateStatementException;
import org.jdbi.v3.tweak.RewrittenStatement;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestHashPrefixStatementRewriter
{
    private HashPrefixStatementRewriter rw;

    @Before
    public void setUp() throws Exception
    {
        this.rw = new HashPrefixStatementRewriter();
    }


    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from something\n where id = #id", new Binding(),
                                            new StatementContext());
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("~* #boo '#nope' _%&^& *@ #id", new Binding(),
                                            new StatementContext());
        assertEquals("~* ? '#nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("#bo0 '#nope' _%&^& *@ #id", new Binding(),
                                            new StatementContext());
        assertEquals("? '#nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from v$session", new Binding(),
                                            new StatementContext());
        assertEquals("select * from v$session", rws.getSql());
    }

    @Test
    public void testColonIsLiteral() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from foo where id = :id", new Binding(),
                                            new StatementContext());
        assertEquals("select * from foo where id = :id", rws.getSql());
    }

    @Test
    public void testBacktickOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from `v$session", new Binding(),
                                            new StatementContext());
        assertEquals("select * from `v$session", rws.getSql());
    }

    @Test
    public void testBailsOutOnInvalidInput() throws Exception
    {
        try {
            rw.rewrite("select * from something\n where id = #\u0087\u008e\u0092\u0097\u009c", new Binding(),
                       new StatementContext());

            Assert.fail("Expected 'UnableToCreateStatementException' but got none");
        }
        catch (UnableToCreateStatementException e) {
        }
    }
}

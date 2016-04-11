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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.exceptions.UnableToCreateStatementException;
import org.skife.jdbi.v2.tweak.RewrittenStatement;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class TestColonStatementRewriter
{
    private ColonPrefixNamedParamStatementRewriter rw;

    @Before
    public void setUp() throws Exception
    {
        this.rw = new ColonPrefixNamedParamStatementRewriter();
    }

    private RewrittenStatement rewrite(String sql)
    {
        return rw.rewrite(sql,
                new Binding(),
                new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from something\n where id = :id");
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rewrite("~* :boo ':nope' _%&^& *@ :id");
        assertEquals("~* ? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rewrite(":bo0 ':nope' _%&^& *@ :id");
        assertEquals("? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from v$session");
        assertEquals("select * from v$session", rws.getSql());
    }

    @Test
    public void testHashInColumnNameOkay() throws Exception
    {
       RewrittenStatement rws = rewrite("select column# from thetable where id = :id");
       assertEquals("select column# from thetable where id = ?", rws.getSql());
    }

    @Test
    public void testBacktickOkay() throws Exception
    {
        RewrittenStatement rws = rewrite("select * from `v$session");
        assertEquals("select * from `v$session", rws.getSql());
    }

    @Test
    public void testDoubleColon() throws Exception
    {
        final String doubleColon = "select 1::int";
        RewrittenStatement rws = rewrite(doubleColon);
        assertEquals(doubleColon, rws.getSql());
    }

    @Test(expected = UnableToCreateStatementException.class)
    public void testBailsOutOnInvalidInput() throws Exception
    {
        rewrite("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c");
        Assert.fail("Expected 'UnableToCreateStatementException' but got none");
    }

    @Test
    public void testCachesRewrittenStatements() throws Exception
    {
        final AtomicInteger ctr = new AtomicInteger(0);
        rw = new ColonPrefixNamedParamStatementRewriter()
        {
            @Override
            ParsedStatement parseString(final String sql) throws IllegalArgumentException
            {
                ctr.incrementAndGet();
                return super.parseString(sql);
            }
        };

        rewrite("insert into something (id, name) values (:id, :name)");

        assertEquals(1, ctr.get());

        rewrite("insert into something (id, name) values (:id, :name)");

        assertEquals(1, ctr.get());
    }

    public void testCommentQuote() throws Exception
    {
        rewrite("select 1 /* ' \" */");
    }
}

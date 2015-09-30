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
import org.skife.jdbi.v2.tweak.StatementRewriter;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class TestColonStatementRewriter
{
    private ColonPrefixNamedParamStatementRewriter rw;

    @Before
    public void setUp() throws Exception
    {
        this.rw = new ColonPrefixNamedParamStatementRewriter();
    }

    @Test
    public void testNewlinesOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from something\n where id = :id", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
        assertEquals("select * from something\n where id = ?", rws.getSql());
    }

    @Test
    public void testOddCharacters() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("~* :boo ':nope' _%&^& *@ :id", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
        assertEquals("~* ? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testNumbers() throws Exception
    {
        RewrittenStatement rws = rw.rewrite(":bo0 ':nope' _%&^& *@ :id", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
        assertEquals("? ':nope' _%&^& *@ ?", rws.getSql());
    }

    @Test
    public void testDollarSignOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from v$session", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
        assertEquals("select * from v$session", rws.getSql());
    }

    @Test
    public void testHashInColumnNameOkay() throws Exception
    {
       RewrittenStatement rws = rw.rewrite("select column# from thetable where id = :id", new Binding(),
                                           new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
       assertEquals("select column# from thetable where id = ?", rws.getSql());
    }

    @Test
    public void testBacktickOkay() throws Exception
    {
        RewrittenStatement rws = rw.rewrite("select * from `v$session", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));
        assertEquals("select * from `v$session", rws.getSql());
    }

    @Test
    public void testBailsOutOnInvalidInput() throws Exception
    {
        try {
            rw.rewrite("select * from something\n where id = :\u0087\u008e\u0092\u0097\u009c", new Binding(),
                                            new ConcreteStatementContext(new HashMap<String, Object>(), new MappingRegistry()));

            Assert.fail("Expected 'UnableToCreateStatementException' but got none");
        }
        catch (UnableToCreateStatementException e) {
        }
    }

    @Test
    public void testCachesRewrittenStatements() throws Exception
    {
        final AtomicInteger ctr = new AtomicInteger(0);
        ColonPrefixNamedParamStatementRewriter rw = new ColonPrefixNamedParamStatementRewriter()
        {
            @Override
            ParsedStatement parseString(final String sql) throws IllegalArgumentException
            {
                ctr.incrementAndGet();
                return super.parseString(sql);
            }
        };

        rw.rewrite("insert into something (id, name) values (:id, :name)", new Binding(),
                new ConcreteStatementContext(new HashMap<String, Object>(), null));

        assertEquals(1, ctr.get());

        rw.rewrite("insert into something (id, name) values (:id, :name)", new Binding(),
                new ConcreteStatementContext(new HashMap<String, Object>(), null));

        assertEquals(1, ctr.get());
    }

}

/* Copyright 2004-2005 Brian McCallister
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
package org.skife.jdbi;

import junit.framework.TestCase;

public class TestParams extends TestCase
{
    public void testExtractParams() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :id");
        final String[] params = cache.getNamedParams();
        assertEquals(1, params.length);
        assertEquals("id", params[0]);
    }

    public void testExtractParams2() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :id and baz = :wom_blat");
        final String[] params = cache.getNamedParams();
        assertEquals(2, params.length);
        assertEquals("id", params[0]);
        assertEquals("wom_blat", params[1]);
    }

    public void testReplaceParams() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from baz where id = :wombat and cat like :food");
        assertEquals("select foo from baz where id = ? and cat like ?", cache.getSubstitutedSql());
    }

    public void testAwkwardlyNamedParams() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like 'http://www'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like 'http://www'", parsed);
    }

    public void testEvenMoreAwkwardlyNamedParams() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like 'http :www'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like 'http :www'", parsed);
    }

    public void testEvenMoreAwkwardlyNamedParams2() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like 'http :www :hre'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like 'http :www :hre'", parsed);
    }

    public void testEvenMoreAwkwardlyNamedParams3() throws Exception
    {
        final StatementParser cache
                = new StatementParser("'http' and id = :foo");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("'http' and id = ?", parsed);
    }

    public void testEvenMoreAwkwardlyNamedParams4() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like 'http :www'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like 'http :www'", parsed);
    }

    public void testYetMoreAwkwardlyNamedParams() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like 'http : www'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like 'http : www'", parsed);
    }

    public void testOneMoreAwkwardParam() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like 'http:www'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like 'http:www'", parsed);
    }

    public void testOneMoreWhyNotAwkwardParam() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = :foo and it like ':'");
        final String parsed = cache.getSubstitutedSql();
        assertEquals("select foo from bar where id = ? and it like ':'", parsed);
    }

    public void testNoParamsOrQuotes() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = 7");
        assertEquals("select foo from bar where id = 7", cache.getSubstitutedSql());
        assertEquals(0, cache.getNamedParams().length);
    }

    public void testNoParamsWithQuotes() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where id = '7'");
        assertEquals("select foo from bar where id = '7'", cache.getSubstitutedSql());
        assertEquals(0, cache.getNamedParams().length);
    }

    public void testWonkyStuff() throws Exception
    {
        final StatementParser cache
                = new StatementParser("select foo from bar where (id = :id) and (name = :name)");
        assertEquals("select foo from bar where (id = ?) and (name = ?)", cache.getSubstitutedSql());
        String[] params = cache.getNamedParams();
        assertEquals("id", params[0]);
        assertEquals("name", params[1]);
    }

    public void testMoreWonkyStuff() throws Exception
    {
        final StatementParser cache
                = new StatementParser("insert into something (id, name) values (:id, :name)");
        assertEquals("insert into something (id, name) values (?, ?)", cache.getSubstitutedSql());
        String[] params = cache.getNamedParams();
        assertEquals("id", params[0]);
        assertEquals("name", params[1]);
    }
}

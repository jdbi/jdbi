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

import org.junit.Assert;
import org.junit.Test;

public class TestEscapedCharacters
{
    private final ColonPrefixNamedParamStatementRewriter rewriter = new ColonPrefixNamedParamStatementRewriter();

    private String parseString(final String src)
    {
        return rewriter.parseString(src, new ColonPrefixNamedParamStatementRewriter.ParsedStatement());
    }

    @Test
    public void testSimpleString()
    {
        Assert.assertEquals("hello, world", parseString("hello, world"));
    }

    @Test
    public void testSimpleSql()
    {
        Assert.assertEquals("insert into foo (xyz) values (?)", parseString("insert into foo (xyz) values (:bar)"));
    }

    @Test
    public void testEscapedSql()
    {
        Assert.assertEquals("insert into foo (xyz) values (?::some_strange_type)", parseString("insert into foo (xyz) values (:bar\\:\\:some_strange_type)"));
    }
}

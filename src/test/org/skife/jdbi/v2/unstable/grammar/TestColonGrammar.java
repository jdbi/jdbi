/*
 * Copyright 2004-2006 Brian McCallister
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

package org.skife.jdbi.v2.unstable.grammar;

import antlr.CharScanner;
import org.skife.jdbi.rewriter.colon.ColonStatementLexer;
import static org.skife.jdbi.rewriter.colon.ColonStatementLexerTokenTypes.*;

import java.io.Reader;

/**
 *
 */
public class TestColonGrammar extends GrammarTestCase
{

    public void testNamedOnly() throws Exception
    {
        expect("select id from something where name like ':foo' and id = :id and name like :name",
               LITERAL, QUOTED_TEXT, LITERAL, NAMED_PARAM, LITERAL, NAMED_PARAM, EOF);
    }

    public void testMixed() throws Exception
    {
        expect("select id from something where name like ':foo' and id = ? and name like :name",
               LITERAL, QUOTED_TEXT, LITERAL, POSITIONAAL_PARAM, LITERAL, NAMED_PARAM, EOF);
    }

    public void testThisBrokeATest() throws Exception
    {
        expect("insert into something (id, name) values (:id, :name)",
               LITERAL, NAMED_PARAM, LITERAL, NAMED_PARAM, LITERAL, EOF);
    }

    public void testExclamationWorks() throws Exception
    {
        expect("select1 != 2 from dual", LITERAL, EOF);
    }

    protected String nameOf(int type)
    {
        switch (type) {
            case LITERAL:
                return "LITERAL";
            case QUOTED_TEXT:
                return "QUOTED_TEXT";
            case NAMED_PARAM:
                return "NAMED_PARAM";
            case EOF:
                return "EOF";
        }
        return String.valueOf(type);
    }


    protected CharScanner createLexer(Reader r)
    {
        return new ColonStatementLexer(r);
    }
}

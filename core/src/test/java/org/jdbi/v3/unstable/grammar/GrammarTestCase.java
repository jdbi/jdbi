/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.jdbi.v3.unstable.grammar;

import org.antlr.runtime.Lexer;
import org.antlr.runtime.Token;

import junit.framework.TestCase;

/**
 *
 */
public abstract class GrammarTestCase extends TestCase
{

    public void expect(String s, int... tokens) throws Exception
    {
        Lexer lexer = createLexer(s);
        for (int token : tokens) {
            Token t = lexer.nextToken();
            assertEquals(String.format("Expected %s, got %s, with '%s'", nameOf(token), nameOf(t.getType()), t.getText()),
                         token, t.getType());
        }
    }

    protected abstract Lexer createLexer(String s);

    protected abstract String nameOf(int type);
}

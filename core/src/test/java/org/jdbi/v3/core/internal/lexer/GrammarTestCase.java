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
package org.jdbi.v3.core.internal.lexer;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class GrammarTestCase {
    public void expect(String s, int... tokens) {
        Lexer lexer = createLexer(s);
        for (int token : tokens) {
            Token t = lexer.nextToken();
            assertThat(t.getType()).isEqualTo(token)
                    .withFailMessage("Expected %s, got %s, with '%s'", nameOf(token), nameOf(t.getType()), t.getText());
        }
    }

    protected abstract Lexer createLexer(String s);

    protected abstract String nameOf(int type);
}

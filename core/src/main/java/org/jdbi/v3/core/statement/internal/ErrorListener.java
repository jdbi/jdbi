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
package org.jdbi.v3.core.statement.internal;

import java.util.BitSet;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.jdbi.v3.core.statement.UnableToCreateStatementException;

public class ErrorListener implements ANTLRErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
            int line, int charPositionInLine, String msg,
            RecognitionException e) {
        throw new UnableToCreateStatementException(e);
    }
    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex,
            int stopIndex, boolean exact, BitSet ambigAlts,
            ATNConfigSet configs) {
        throw new UnsupportedOperationException("not used by lexers");
    }
    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa,
            int startIndex, int stopIndex, BitSet conflictingAlts,
            ATNConfigSet configs) {
        throw new UnsupportedOperationException("not used by lexers");
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa,
            int startIndex, int stopIndex, int prediction,
            ATNConfigSet configs) {
        throw new UnsupportedOperationException("not used by lexers");
    }
}

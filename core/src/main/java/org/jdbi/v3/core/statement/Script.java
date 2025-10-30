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
package org.jdbi.v3.core.statement;

import java.util.List;

import org.antlr.v4.runtime.CharStreams;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.internal.SqlScriptParser;
import org.jdbi.v3.core.internal.SqlScriptParser.ScriptTokenHandler;

/**
 * Represents a number of SQL statements delimited by semicolon which will be executed in order in a batch statement.
 */
public class Script extends SqlStatement<Script> {
    private final boolean requireSemicolon;

    public Script(Handle handle, CharSequence sql) {
        super(handle, sql);
        this.requireSemicolon = handle.getConfig(SqlStatements.class).isScriptStatementsNeedSemicolon();
    }

    /**
     * Execute this script in a batch statement
     *
     * @return an array of ints which are the results of each statement in the script
     */
    public int[] execute() {
        final List<String> statements = getStatements();
        try (Batch b = getHandle().createBatch()) {
            statements.forEach(b::add);
            return b.execute();
        }
    }

    /**
     * Execute this script as a set of separate statements
     */
    public void executeAsSeparateStatements() {
        for (String s : getStatements()) {
            getHandle().execute(s);
        }
    }

    /**
     * Locate the Script and split it into statements.
     * @return the split statements
     */
    public List<String> getStatements() {
        var templateEngine = getConfig(SqlStatements.class).getTemplateEngine();
        return splitToStatements(templateEngine.render(getSql(), getContext()));
    }

    private List<String> splitToStatements(String script) {
        ScriptTokenHandler scriptTokenHandler = new ScriptTokenHandler(this.requireSemicolon);
        String lastStatement = new SqlScriptParser(scriptTokenHandler)
            .parse(CharStreams.fromString(script));

        return scriptTokenHandler.addStatement(lastStatement);
    }
}

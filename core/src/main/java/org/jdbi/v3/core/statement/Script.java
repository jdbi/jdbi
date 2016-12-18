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

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.internal.SqlScriptParser;

/**
 * Represents a number of SQL statements which will be executed in a batch statement.
 */
public class Script
{
    public static Script create(Handle handle, String sql) {
        return new Script(handle, sql);
    }

    private final Handle handle;
    private final String sql;

    Script(Handle h, String sql)
    {
        this.handle = h;
        this.sql = sql;
    }

    /**
     * Execute this script in a batch statement
     *
     * @return an array of ints which are the results of each statement in the script
     */
    public int[] execute() {
        final List<String> statements = getStatements();
        Batch b = handle.createBatch();
        statements.forEach(b::add);
        return b.execute();
    }

    /**
     * Execute this script as a set of separate statements
     */
    public void executeAsSeparateStatements() {
        for (String s : getStatements()) {
            handle.execute(s);
        }
    }

    private List<String> getStatements() {
        return splitToStatements(sql);
    }

    private List<String> splitToStatements(String script) {
        final List<String> statements = new ArrayList<>();
        String lastStatement = new SqlScriptParser((t, sb) -> {
            addStatement(sb.toString(), statements);
            sb.setLength(0);
        }).parse(new ANTLRStringStream(script));
        addStatement(lastStatement, statements);

        return statements;
    }

    private void addStatement(String statement, List<String> statements) {
        String trimmedStatement = statement.trim();
        if (trimmedStatement.isEmpty()) {
            return;
        }
        statements.add(trimmedStatement);
    }
}

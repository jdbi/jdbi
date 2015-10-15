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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.Token;
import org.jdbi.v3.exceptions.UnableToExecuteStatementException;
import org.jdbi.v3.tweak.StatementLocator;

/**
 * Represents a number of SQL statements which will be executed in a batch statement.
 */
public class Script
{

    private final Handle handle;
    private final StatementLocator locator;
    private final String name;
    private final Map<String, Object> globalStatementAttributes;

    Script(Handle h, StatementLocator locator, String name, Map<String, Object> globalStatementAttributes)
    {
        this.handle = h;
        this.locator = locator;
        this.name = name;
        this.globalStatementAttributes = globalStatementAttributes;
    }

    /**
     * Execute this script in a batch statement
     *
     * @return an array of ints which are the results of each statement in the script
     */
    public int[] execute() {
        final List<String> statements = getStatements();
        Batch b = handle.createBatch();
        for (String s : statements) {
            b.add(s);
        }
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
        final String script;
        final StatementContext ctx = new ConcreteStatementContext(globalStatementAttributes);
        try {
            script = locator.locate(name, ctx);
        } catch (Exception e) {
            throw new UnableToExecuteStatementException(String.format("Error while loading script [%s]", name), e, ctx);
        }

        return splitToStatements(script);
    }

    private List<String> splitToStatements(String script) {
        final List<String> statements = new ArrayList<String>();
        String lastStatement = new SqlScriptParser(new SqlScriptParser.TokenHandler() {
            @Override
            public void handle(Token t, StringBuilder sb) {
                addStatement(sb.toString(), statements);
                sb.setLength(0);
            }
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

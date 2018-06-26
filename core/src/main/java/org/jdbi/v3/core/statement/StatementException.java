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

import org.jdbi.v3.core.JdbiException;

/**
 * Superclass for exceptions thrown while trying to execute a statement.
 */
public abstract class StatementException extends JdbiException {
    private static final long serialVersionUID = 1L;

    private final StatementContext statementContext;

    public StatementException(String string, Throwable throwable, StatementContext ctx) {
        super(string, throwable);
        this.statementContext = ctx;
    }

    public StatementException(String string, Throwable throwable) {
        super(string, throwable);
        this.statementContext = null;
    }

    public StatementException(Throwable cause, StatementContext ctx) {
        super(cause);
        this.statementContext = ctx;
    }

    public StatementException(String message, StatementContext ctx) {
        super(message);
        this.statementContext = ctx;
    }

    public StatementException(String string) {
        super(string);
        this.statementContext = null;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    public String getShortMessage() {
        return super.getMessage();
    }

    @Override
    public String getMessage() {
        String base = super.getMessage();
        StatementContext ctx = getStatementContext();
        if (ctx == null) {
            return base;
        } else {
            return String.format("%s [statement:\"%s\", rewritten:\"%s\", parsed:\"%s\", arguments:%s]",
                                 base,
                                 ctx.getRawSql(),
                                 ctx.getRenderedSql(),
                                 ctx.getParsedSql(),
                                 ctx.getBinding());
        }
    }
}

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
package org.skife.jdbi.v2.exceptions;

import org.skife.jdbi.v2.StatementContext;

/**
 *
 */
public abstract class StatementException extends DBIException
{
    private final StatementContext statementContext;

    public StatementException(String string, Throwable throwable, StatementContext ctx) {
        super(string, throwable);
        this.statementContext = ctx;
    }

    public StatementException(Throwable cause, StatementContext ctx) {
        super(cause);
        this.statementContext = ctx;
    }

    public StatementException(String message, StatementContext ctx) {
        super(message);
        this.statementContext = ctx;
    }

    /**
     * @deprecated
     */
    public StatementException(String string, Throwable throwable) {
        super(string, throwable);
        this.statementContext = null;
    }

    /**
     * @deprecated
     */
    public StatementException(Throwable cause) {
        super(cause);
        this.statementContext = null;
    }

    /**
     * @deprecated
     */
    public StatementException(String message) {
        super(message);
        this.statementContext = null;
    }

    public StatementContext getStatementContext() {
        return statementContext;
    }

    @Override
    public String getMessage() {
        String base = super.getMessage();
        StatementContext ctx = getStatementContext();
        if (ctx == null) {
            return base;
        }
        else {
            return String.format("%s [statement:\"%s\", located:\"%s\", rewritten:\"%s\", arguments:%s]",
                                 base,
                                 ctx.getRawSql(),
                                 ctx.getLocatedSql(),
                                 ctx.getRewrittenSql(),
                                 ctx.getBinding());
        }
    }
}

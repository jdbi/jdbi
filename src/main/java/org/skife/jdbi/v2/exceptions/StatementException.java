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

    public StatementContext getStatementContext() {
        return statementContext;
    }
}

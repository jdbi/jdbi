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

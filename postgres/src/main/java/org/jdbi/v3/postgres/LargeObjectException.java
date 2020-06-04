package org.jdbi.v3.postgres;

import org.jdbi.v3.core.statement.StatementException;

public class LargeObjectException extends StatementException {
    private static final long serialVersionUID = 1L;

    public LargeObjectException(Throwable cause) {
        super(cause);
    }
}

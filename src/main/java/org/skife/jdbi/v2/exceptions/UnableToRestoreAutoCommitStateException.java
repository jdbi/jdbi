package org.skife.jdbi.v2.exceptions;

public class UnableToRestoreAutoCommitStateException  extends DBIException {

    public UnableToRestoreAutoCommitStateException(Throwable throwable) {
        super(throwable);
    }
}

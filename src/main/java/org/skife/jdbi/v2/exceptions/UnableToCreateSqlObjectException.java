package org.skife.jdbi.v2.exceptions;

public class UnableToCreateSqlObjectException extends DBIException {

    public UnableToCreateSqlObjectException(String message) {
        super(message);
    }
}
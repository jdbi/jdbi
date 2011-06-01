package org.skife.jdbi.v2.exceptions;

import java.sql.SQLException;

public class UnableToManipulateTransactionIsolationLevelException extends DBIException
{
    public UnableToManipulateTransactionIsolationLevelException(int i, SQLException e)
    {
        super("Unable to set isolation level to " + i, e);
    }

    public UnableToManipulateTransactionIsolationLevelException(String msg, SQLException e)
    {
        super(msg, e);
    }
}

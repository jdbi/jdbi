package org.skife.jdbi.v2;

import java.sql.Connection;

public enum TransactionIsolationLevel
{
    READ_UNCOMMITTED(Connection.TRANSACTION_READ_UNCOMMITTED),
    READ_COMMITTED(Connection.TRANSACTION_READ_COMMITTED),
    REPEATABLE_READ(Connection.TRANSACTION_REPEATABLE_READ),
    NONE(Connection.TRANSACTION_NONE),
    SERIALIZABLE(Connection.TRANSACTION_SERIALIZABLE),
    INVALID_LEVEL(Integer.MIN_VALUE);

    private final int value;

    TransactionIsolationLevel(int value)
    {
        this.value = value;
    }

    public int intValue()
    {
        return this.value;
    }

    public static TransactionIsolationLevel valueOf(int val) {
        switch (val) {
            case Connection.TRANSACTION_READ_UNCOMMITTED: return READ_UNCOMMITTED;
            case Connection.TRANSACTION_READ_COMMITTED: return READ_COMMITTED;
            case Connection.TRANSACTION_NONE: return NONE;
            case Connection.TRANSACTION_REPEATABLE_READ: return REPEATABLE_READ;
            case Connection.TRANSACTION_SERIALIZABLE: return SERIALIZABLE;
            default: return INVALID_LEVEL;
        }
    }
}
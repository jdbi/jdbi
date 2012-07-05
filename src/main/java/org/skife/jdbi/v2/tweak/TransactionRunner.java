package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;

/**
 * Interface that lets you customize the transaction behavior of a Handle
 */
public interface TransactionRunner
{
    <ReturnType> ReturnType inTransaction(Handle handle, TransactionCallback<ReturnType> callback);
    <ReturnType> ReturnType inTransaction(Handle handle, TransactionIsolationLevel level, TransactionCallback<ReturnType> callback);
}

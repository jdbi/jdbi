package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.Handle;

/**
 * Interface used to receive sql logging events.
 */
public interface SQLLog
{
    /**
     * Called when a transaction is started
     */
    public void logBeginTransaction(Handle h);

    /**
     * Called when a transaction is committed
     */
    public void logCommitTransaction(Handle h);

    /**
     * Called when a transaction is committed
     */
    public void logRollbackTransaction(Handle h);

    /**
     * Called when a handle is opened from a DBI instance
     */
    public void logObtainHandle(Handle h);

    /**
     * Called when a handle is closed
     */
    public void logReleaseHandle(Handle h);

    /**
     * Called to log typical sql statements
     * @param sql the actual sql being exected
     */
    public void logSQL(String sql);

    /**
     * Called to log a prepared batch execution
     * @param sql The sql for the prepared batch
     * @param count the number of elements in the prepared batch
     */
    public void logPreparedBatch(String sql, int count);

    /**
     * Factory method used to obtain a SQLLog.BatchLogger which will be used to log
     * a specific batch statement.
     *
     * @return an instance of BatchLogger which will be used to log this batch
     */
    public BatchLogger logBatch();

    /**
     * Called when a transaction is checkpointed
     * @param name the checkpoint name
     */
    void logCheckpointTransaction(Handle h, String name);

    /**
     * Called when a transaction checkpoint is released
     * @param name the checkpoint name
     */
    void logReleaseCheckpointTransaction(Handle h, String name);

    /**
     * Called when a transaction checkpoint is rolled back to
     * @param name the checkpoint name
     */
    void logRollbackToCheckpoint(Handle h, String checkpointName);

    /**
     * Instances of this are used to log batch statements. SQLLog#logBatch will return one of these.
     * A new one will be requested for each batch execution.
     */
    public interface BatchLogger
    {
        /**
         * Called once for each statement in the batch
         * @param sql sql for the statement
         */
        public void add(String sql);

        /**
         * Called when all statements have been passed to add()
         */
        public void log();
    }
}

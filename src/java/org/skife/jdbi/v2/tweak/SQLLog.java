package org.skife.jdbi.v2.tweak;

/**
 * Interface used to receive sql logging events.
 */
public interface SQLLog
{
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

package org.skife.jdbi.v2.logging;

import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.Handle;

/**
 * Default SQLLog implementation, does nothing
 */
public final class NoOpLog implements SQLLog
{
    public void logBeginTransaction()
    {
    }

    public void logCommitTransaction()
    {
    }

    public void logRollbackTransaction()
    {
    }

    public void logObtainHandle(Handle h)
    {
    }

    public void logReleaseHandle(Handle h)
    {
    }

    public final void logSQL(String sql)
    {
    }

    public final void logPreparedBatch(String sql, int count)
    {
    }

    public final BatchLogger logBatch()
    {
        return batch;
    }

    public void logCheckpointTransaction(String name)
    {
    }

    public void logReleaseCheckpointTransaction(String name)
    {
    }

    public void logRollbackToCheckpoint(String checkpointName)
    {
    }

    final static BatchLogger batch = new BatchLogger() {

        public final void add(String sql)
        {
        }

        public final void log()
        {
        }
    };
}

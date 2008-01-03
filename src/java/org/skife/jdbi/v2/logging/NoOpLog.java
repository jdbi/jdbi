package org.skife.jdbi.v2.logging;

import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.Handle;

/**
 * Default SQLLog implementation, does nothing
 */
public final class NoOpLog implements SQLLog
{

    final static BatchLogger batch = new BatchLogger() {

        public final void add(String sql)
        {
        }

        public final void log()
        {
        }
    };

    public void logBeginTransaction(Handle h)
    {
    }

    public void logCommitTransaction(Handle h)
    {
    }

    public void logRollbackTransaction(Handle h)
    {
    }

    public void logObtainHandle(Handle h)
    {
    }

    public void logReleaseHandle(Handle h)
    {
    }

    public void logSQL(String sql)
    {
    }

    public void logPreparedBatch(String sql, int count)
    {
    }

    public BatchLogger logBatch()
    {
        return null;
    }

    public void logCheckpointTransaction(Handle h, String name)
    {
    }

    public void logReleaseCheckpointTransaction(Handle h, String name)
    {
    }

    public void logRollbackToCheckpoint(Handle h, String checkpointName)
    {
    }
}

package org.skife.jdbi.v2.logging;

import org.skife.jdbi.v2.tweak.SQLLog;

/**
 * Default SQLLog implementation, does nothing
 */
public final class NoOpLog implements SQLLog
{
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

    final static BatchLogger batch = new BatchLogger() {

        public final void add(String sql)
        {
        }

        public final void log()
        {
        }
    };
}

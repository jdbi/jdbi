package org.skife.jdbi.v2.logging;

import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.Handle;

/**
 * Convenience class which handles log statement formatting
 */
public abstract class FormattedLog implements SQLLog
{
    public final void logSQL(String sql)
    {
        if (isEnabled()) log(String.format("statement:[%s]", sql));
    }

    /**
     * Used to ask implementations if logging is enabled.
     *
     * @return true if statement logging is enabled
     */
    protected abstract boolean isEnabled();

    /**
     * Log the statement passed in
     * @param msg the message to log
     */
    protected abstract void log(String msg);


    public final void logPreparedBatch(String sql, int count)
    {
        if (isEnabled()) log(String.format("prepared batch with %d parts:[%s]", count, sql));
    }

    public final BatchLogger logBatch()
    {
        if (isEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("batch:[");
            return new BatchLogger()
            {
                private boolean added = false;

                public final void add(String sql)
                {
                    added = true;
                    builder.append("[").append(sql).append("], ");
                }

                public final void log()
                {
                    if (added) {
                        builder.delete(builder.length() - 2, builder.length());
                    }
                    builder.append("]");
                    FormattedLog.this.log(builder.toString());
                }
            };
        }
        else {
            return NoOpLog.batch;
        }
    }

    public void logBeginTransaction()
    {
        log("begin transaction");
    }

    public void logCommitTransaction()
    {
        log("commit transaction");
    }

    public void logRollbackTransaction()
    {
        log("rollback transaction");
    }

    public void logObtainHandle(Handle h)
    {
        if (this.isEnabled()) {
            log(String.format("Handle [%s] obtained", h));
        }
    }

    public void logReleaseHandle(Handle h)
    {
        if (this.isEnabled()) {
            log(String.format("Handle [%s] released", h));
        }
    }

    public void logCheckpointTransaction(String name)
    {
        if (this.isEnabled()) {
            log(String.format("checkpoint [%s] created", name));
        }
    }

    public void logReleaseCheckpointTransaction(String name)
    {
        if (this.isEnabled()) {
            log(String.format("checkpoint [%s] released", name));
        }
    }

    public void logRollbackToCheckpoint(String checkpointName)
    {
        if (this.isEnabled()) {
            log(String.format("checkpoint [%s] rolled back", checkpointName));
        }
    }
}

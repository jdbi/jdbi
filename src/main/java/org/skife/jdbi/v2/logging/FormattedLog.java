/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2.logging;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.tweak.SQLLog;

/**
 * Convenience class which handles log statement formatting
 */
public abstract class FormattedLog implements SQLLog
{
    @Override
    public final void logSQL(long time, String sql)
    {
        if (isEnabled()) {
            log(String.format("statement:[%s] took %d millis", sql, time));
        }
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


    @Override
    public final void logPreparedBatch(long time, String sql, int count)
    {
        if (isEnabled()) {
            log(String.format("prepared batch with %d parts:[%s] took %d millis", count, sql, time));
        }
    }

    @Override
    public final BatchLogger logBatch()
    {
        if (isEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("batch:[");
            return new BatchLogger()
            {
                private boolean added = false;

                @Override
                public final void add(String sql)
                {
                    added = true;
                    builder.append("[").append(sql).append("], ");
                }

                @Override
                public final void log(long time)
                {
                    if (added) {
                        builder.delete(builder.length() - 2, builder.length());
                    }
                    builder.append("]");
                    FormattedLog.this.log(String.format("%s took %d millis", builder.toString(), time));
                }
            };
        }
        else {
            return NoOpLog.batch;
        }
    }

    @Override
    public void logBeginTransaction(Handle h)
    {
        if (isEnabled()) {
            log(String.format("begin transaction on [%s]", h));
        }
    }

    @Override
    public void logCommitTransaction(long time, Handle h)
    {
        if (isEnabled()) {
            log(String.format("commit transaction on [%s] took %d millis", h, time));
        }
    }

    @Override
    public void logRollbackTransaction(long time, Handle h)
    {
        if (isEnabled()) {
            log(String.format("rollback transaction on [%s] took %d millis", h, time));
        }
    }

    @Override
    public void logObtainHandle(long time, Handle h)
    {
        if (this.isEnabled()) {
            log(String.format("Handle [%s] obtained in %d millis", h, time));
        }
    }

    @Override
    public void logReleaseHandle(Handle h)
    {
        if (this.isEnabled()) {
            log(String.format("Handle [%s] released", h));
        }
    }

    @Override
    public void logCheckpointTransaction(Handle h, String name)
    {
        if (this.isEnabled()) {
            log(String.format("checkpoint [%s] created on [%s]", name, h));
        }
    }

    @Override
    public void logReleaseCheckpointTransaction(Handle h, String name)
    {
        if (this.isEnabled()) {
            log(String.format("checkpoint [%s] on [%s] released", name, h));
        }
    }

    @Override
    public void logRollbackToCheckpoint(long time, Handle h, String checkpointName)
    {
        if (this.isEnabled()) {
            log(String.format("checkpoint [%s] on [%s] rolled back in %d millis", checkpointName, h, time));
        }
    }
}

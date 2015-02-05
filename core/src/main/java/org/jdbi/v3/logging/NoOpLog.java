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
package org.jdbi.v3.logging;

import org.jdbi.v3.Handle;
import org.jdbi.v3.tweak.SQLLog;

/**
 * Default SQLLog implementation, does nothing
 */
public final class NoOpLog implements SQLLog
{

    static final BatchLogger batch = new BatchLogger() {

        @Override
        public void add(String sql)
        {
        }

        @Override
        public void log(long time)
        {
        }
    };

    @Override
    public void logBeginTransaction(Handle h)
    {
    }

    @Override
    public void logCommitTransaction(long time, Handle h)
    {
    }

    @Override
    public void logRollbackTransaction(long time, Handle h)
    {
    }

    @Override
    public void logObtainHandle(long time, Handle h)
    {
    }

    @Override
    public void logReleaseHandle(Handle h)
    {
    }

    @Override
    public void logSQL(long time, String sql)
    {
    }

    @Override
    public void logPreparedBatch(long time, String sql, int count)
    {
    }

    @Override
    public BatchLogger logBatch()
    {
        return batch;
    }

    @Override
    public void logCheckpointTransaction(Handle h, String name)
    {
    }

    @Override
    public void logReleaseCheckpointTransaction(Handle h, String name)
    {
    }

    @Override
    public void logRollbackToCheckpoint(long time, Handle h, String checkpointName)
    {
    }
}

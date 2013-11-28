/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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

    final static BatchLogger batch = new BatchLogger() {

        public final void add(String sql)
        {
        }

        public final void log(long time)
        {
        }
    };

    public void logBeginTransaction(Handle h)
    {
    }

    public void logCommitTransaction(long time, Handle h)
    {
    }

    public void logRollbackTransaction(long time, Handle h)
    {
    }

    public void logObtainHandle(long time, Handle h)
    {
    }

    public void logReleaseHandle(Handle h)
    {
    }

    public void logSQL(long time, String sql)
    {
    }

    public void logPreparedBatch(long time, String sql, int count)
    {
    }

    public BatchLogger logBatch()
    {
        return batch;
    }

    public void logCheckpointTransaction(Handle h, String name)
    {
    }

    public void logReleaseCheckpointTransaction(Handle h, String name)
    {
    }

    public void logRollbackToCheckpoint(long time, Handle h, String checkpointName)
    {
    }
}

/*
 * Copyright 2004 - 2011 Brian McCallister
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
    public void logCommitTransaction(long time, Handle h);

    /**
     * Called when a transaction is committed
     */
    public void logRollbackTransaction(long time, Handle h);

    /**
     * Called when a handle is opened from a DBI instance
     */
    public void logObtainHandle(long time, Handle h);

    /**
     * Called when a handle is closed
     */
    public void logReleaseHandle(Handle h);

    /**
     * Called to log typical sql statements
     * @param sql the actual sql being exected
     */
    public void logSQL(long time, String sql);

    /**
     * Called to log a prepared batch execution
     * @param sql The sql for the prepared batch
     * @param count the number of elements in the prepared batch
     */
    public void logPreparedBatch(long time, String sql, int count);

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
     */
    void logRollbackToCheckpoint(long time, Handle h, String checkpointName);

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
        public void log(long time);
    }
}

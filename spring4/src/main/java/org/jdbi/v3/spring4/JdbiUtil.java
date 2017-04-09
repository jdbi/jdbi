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
package org.jdbi.v3.spring4;

import java.util.HashSet;
import java.util.Set;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.Handle;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Utility for working with Jdbi and Spring transaction bound resources
 */
public class JdbiUtil
{
    private static final Set<Handle> TRANSACTIONAL_HANDLES = new HashSet<>();

    /**
     * Obtain a Handle instance, either the transactionally bound one if we are in a transaction,
     * or a new one otherwise.
     * @param db the Jdbi instance from which to obtain the handle
     *
     * @return the Handle instance
     */
    public static Handle getHandle(Jdbi db)
    {
        Handle bound = (Handle) TransactionSynchronizationManager.getResource(db);
        if (bound == null) {
            bound = db.open();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.bindResource(db, bound);
                TransactionSynchronizationManager.registerSynchronization(new Adapter(db, bound));
                TRANSACTIONAL_HANDLES.add(bound);
            }
        }
        return bound;
    }

    /**
     * Close a handle if it is not transactionally bound, otherwise no-op
     * @param h the handle to consider closing
     */
    public static void closeIfNeeded(Handle h)
    {
        if (!TRANSACTIONAL_HANDLES.contains(h))
        {
            h.close();
        }
    }

    private static class Adapter extends TransactionSynchronizationAdapter {
        private final Jdbi db;
        private final Handle handle;

        Adapter(Jdbi db, Handle handle) {
            this.db = db;
            this.handle = handle;
        }

        @Override
        public void resume()
        {
            TransactionSynchronizationManager.bindResource(db, handle);
        }

        @Override
        public void suspend()
        {
            TransactionSynchronizationManager.unbindResource(db);
        }

        @Override
        public void beforeCompletion()
        {
            TRANSACTIONAL_HANDLES.remove(handle);
            TransactionSynchronizationManager.unbindResource(db);
        }
    }
}

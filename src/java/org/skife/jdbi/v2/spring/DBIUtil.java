/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.spring;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.IDBI;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility for working with jDBI and Spring transaction bound resources
 */
public class DBIUtil
{
    private final static Set<Handle> TRANSACTIONAL_HANDLES = new HashSet<Handle>();

    /**
     * Obtain a Handle instance, either the transactionally bound one if we are in a transaction,
     * or a new one otherwise.
     * @param dbi the IDBI instance from which to obtain the handle
     */
    public static Handle getHandle(IDBI dbi)
    {
        Handle bound = (Handle) TransactionSynchronizationManager.getResource(dbi);
        if (bound == null) {
            bound = dbi.open();
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.bindResource(dbi, bound);
                TransactionSynchronizationManager.registerSynchronization(new Adapter(dbi, bound));
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
        private final IDBI dbi;
        private final Handle handle;

        Adapter(IDBI dbi, Handle handle) {
            this.dbi = dbi;
            this.handle = handle;
        }

        public void resume()
        {
            TransactionSynchronizationManager.bindResource(dbi, handle);
        }

        public void suspend()
        {
            TransactionSynchronizationManager.unbindResource(dbi);
        }

        public void beforeCompletion()
        {
            TRANSACTIONAL_HANDLES.remove(handle);
            TransactionSynchronizationManager.unbindResource(dbi);
        }
    }
}

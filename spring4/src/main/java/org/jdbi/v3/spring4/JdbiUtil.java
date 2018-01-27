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

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility for working with Jdbi and Spring transaction bound resources
 */
public class JdbiUtil
{
    private static final Set<Handle> UNMANAGED_HANDLES = new HashSet<>();

    /**
     * Obtain a Handle instance, either the transactionally bound one if we are in a transaction,
     * or a new one otherwise.
     * @param jdbi the Jdbi instance from which to obtain the handle
     *
     * @return the Handle instance
     */
    public static Handle getHandle(Jdbi jdbi)
    {
        Handle handle;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            HandleHolder boundHolder = (HandleHolder) TransactionSynchronizationManager.getResource(jdbi);
            handle = boundHolder.getHandle();
        } else {
            handle = jdbi.open();
            UNMANAGED_HANDLES.add(handle);
        }
        return handle;
    }

    /**
     * Close a handle if it is not transactionally bound, otherwise no-op
     * @param handle the handle to consider closing
     */
    public static void closeIfNeeded(Handle handle)
    {
        if (UNMANAGED_HANDLES.contains(handle))
        {
            try {
                handle.close();
            } finally {
                UNMANAGED_HANDLES.remove(handle);
            }
        }
    }
}

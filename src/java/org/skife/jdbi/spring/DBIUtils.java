/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.spring;

import org.skife.jdbi.DBIException;
import org.skife.jdbi.Handle;
import org.skife.jdbi.IDBI;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Utility functions for use with spring transaction systems
 */
public class DBIUtils
{
    /**
     * Obtain a handle from <code>dbi</code>, using the transactionally bound
     * one if there is one, otherwise a new one. If spring managed transactions are
     * in effect than resources will be cleaned up by Spring, otherwise you are
     * responsible for resource cleanup
     */
    public static Handle getHandle(IDBI dbi) throws DBIException
    {
        Handle handle = (Handle) TransactionSynchronizationManager.getResource(dbi);

        if (handle == null)
        {
            handle = dbi.open();

            if (TransactionSynchronizationManager.isSynchronizationActive())
            {
                TransactionSynchronizationManager.bindResource(dbi, handle);
                TransactionSynchronizationManager.registerSynchronization(new HandleSynchronization(handle, dbi));
            }
        }

        return handle;
    }

    /**
     * When used in conjuntion with <code>Handle#getHandle()</code> will only close the
     * handle if it is not bound to an ongoing Spring transaction. If it is bound to a
     * transaction then the Handle will be closed when the transaction completes
     * and this will noop.
     *
     * @param h   Handle to consider closing
     * @param dbi DBI handle was obtained from
     */
    public static void closeHandleIfNecessary(final Handle h, final IDBI dbi)
    {
        Handle handle = (Handle) TransactionSynchronizationManager.getResource(dbi);
        if (handle != h)
        {
            h.close();
        }
    }

    private static class HandleSynchronization extends TransactionSynchronizationAdapter
    {
        private final Handle handle;
        private IDBI dbi;

        private HandleSynchronization(final Handle handle, final IDBI dbi)
        {
            this.handle = handle;
            this.dbi = dbi;
        }

        public void suspend()
        {
            TransactionSynchronizationManager.unbindResource(dbi);
        }

        public void resume()
        {
            TransactionSynchronizationManager.bindResource(dbi, handle);
        }

        public void beforeCompletion()
        {
            TransactionSynchronizationManager.unbindResource(dbi);
            // clearing the statement cache closes everything but the JDBC Connection
            // that will be closed when the Connection is closed by the tx
            handle.clearStatementCache();
        }
    }
}

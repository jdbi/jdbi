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

import org.skife.jdbi.tweak.TransactionHandler;
import org.skife.jdbi.tweak.ConnectionTransactionHandler;
import org.skife.jdbi.Handle;
import org.skife.jdbi.IDBI;
import org.skife.jdbi.DBIException;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SpringTransactionHandler implements TransactionHandler
{
    private final IDBI dbi;
    private final TransactionHandler direct;

    SpringTransactionHandler(IDBI dbi)
    {
        direct = new ConnectionTransactionHandler();
        this.dbi = dbi;
    }

    public void begin(Handle handle)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            final Handle txional = (Handle) TransactionSynchronizationManager.getResource(dbi);
            if (txional == this)
            {
                // no-op
            }
            else
            {
                direct.begin(handle);
            }
        }
        else
        {
            direct.begin(handle);
        }
    }

    public void commit(Handle handle)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            final Handle txional = (Handle) TransactionSynchronizationManager.getResource(dbi);
            if (txional == this)
            {
                // no-op
            }
            else
            {
                direct.commit(handle);
            }
        }
        else
        {
            direct.commit(handle);
        }
    }

    public void rollback(Handle handle)
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            final Handle txional = (Handle) TransactionSynchronizationManager.getResource(dbi);
            if (txional == this)
            {
                throw new DBIException("Rollback Transaction");
            }
            else
            {
                direct.rollback(handle);
            }
        }
        else
        {
            direct.rollback(handle);
        }
    }

    public boolean isInTransaction(Handle handle)
    {
        return direct.isInTransaction(handle);
    }
}

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
import org.skife.jdbi.unstable.decorator.BaseHandleDecorator;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SpringHandleAdaptor extends BaseHandleDecorator
{
    private IDBI dbi;

    public SpringHandleAdaptor(Handle handle, IDBI dbi)
    {
        super(handle);
        this.dbi = dbi;
    }

    public void rollback() throws DBIException
    {
        if (TransactionSynchronizationManager.isSynchronizationActive())
        {
            final Handle txional = (Handle) TransactionSynchronizationManager.getResource(dbi);
            if (txional == this)
            {
                throw new DBIException("Rollback Transaction");
            }
        }
        else
        {
            super.rollback();
        }
    }
}

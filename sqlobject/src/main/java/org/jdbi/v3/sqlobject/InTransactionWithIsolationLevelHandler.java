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
package org.jdbi.v3.sqlobject;

import java.lang.reflect.Method;

import org.jdbi.v3.core.HandleSupplier;
import org.jdbi.v3.sqlobject.mixins.Transactional;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

class InTransactionWithIsolationLevelHandler implements Handler
{
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Object invoke(final Object target, Method method, Object[] args, SqlObjectConfig config, HandleSupplier handle) throws Exception
    {
        final TransactionalCallback callback = (TransactionalCallback) args[1];
        final TransactionIsolationLevel level = (TransactionIsolationLevel) args[0];

        return handle.getHandle().inTransaction(level, (conn, status) -> callback.inTransaction(Transactional.class.cast(target), status));
    }
}

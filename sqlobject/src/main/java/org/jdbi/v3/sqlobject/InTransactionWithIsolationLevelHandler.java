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

import net.sf.cglib.proxy.MethodProxy;

import org.jdbi.v3.Transaction;
import org.jdbi.v3.TransactionIsolationLevel;

class InTransactionWithIsolationLevelHandler implements Handler
{
    @Override
    public Object invoke(HandleDing h, final Object target, Object[] args, MethodProxy mp)
    {
        h.retain("transaction#withlevel");
        try {
            @SuppressWarnings("unchecked")
            final Transaction<Object, Object> t = (Transaction<Object, Object>) args[1];
            final TransactionIsolationLevel level = (TransactionIsolationLevel) args[0];

            return h.getHandle().inTransaction(level, (conn, status) -> t.inTransaction(target, status));
        }
        finally {
            h.release("transaction#withlevel");
        }
    }
}

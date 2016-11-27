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
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.mixins.Transactional;

class TransactionalHelper
{
    static Map<Method, Handler> handlers()
    {
        try {
            Map<Method, Handler> h = new HashMap<>();
            h.put(Transactional.class.getMethod("begin"), new BeginHandler());
            h.put(Transactional.class.getMethod("commit"), new CommitHandler());
            h.put(Transactional.class.getMethod("rollback"), new RollbackHandler());

            h.put(Transactional.class.getMethod("savepoint", String.class), new SavepointHandler());
            h.put(Transactional.class.getMethod("releaseSavepoint", String.class), new ReleaseSavepointHandler());
            h.put(Transactional.class.getMethod("rollbackToSavepoint", String.class), new RollbackSavepointHandler());

            h.put(Transactional.class.getMethod("inTransaction", TransactionalCallback.class), new InTransactionHandler());
            h.put(Transactional.class.getMethod("inTransaction", TransactionIsolationLevel.class, TransactionalCallback.class), new InTransactionWithIsolationLevelHandler());
            return h;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("someone wonkered up the bytecode", e);
        }
    }
}

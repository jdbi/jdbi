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

import org.jdbi.v3.core.OnDemandExtensions;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.mixins.Transactional;

class TransactionalHelper
{
    static Map<Method, Handler> handlers()
    {
        try {
            Map<Method, Handler> h = new HashMap<>();
            h.put(Transactional.class.getMethod("begin"), guard(new BeginHandler()));
            h.put(Transactional.class.getMethod("commit"), guard(new CommitHandler()));
            h.put(Transactional.class.getMethod("rollback"), guard(new RollbackHandler()));

            h.put(Transactional.class.getMethod("savepoint", String.class), guard(new SavepointHandler()));
            h.put(Transactional.class.getMethod("releaseSavepoint", String.class), guard(new ReleaseSavepointHandler()));
            h.put(Transactional.class.getMethod("rollbackToSavepoint", String.class), guard(new RollbackSavepointHandler()));

            h.put(Transactional.class.getMethod("inTransaction", TransactionalCallback.class), new InTransactionHandler());
            h.put(Transactional.class.getMethod("inTransaction", TransactionIsolationLevel.class, TransactionalCallback.class), new InTransactionWithIsolationLevelHandler());
            return h;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("someone wonkered up the bytecode", e);
        }
    }

    /**
     * Forbid the given handler from interacting with {@link OnDemandExtensions}.
     */
    private static Handler guard(Handler handler)
    {
        return (t, m, a, h) -> {
            if (OnDemandExtensions.inOnDemandExtension())
            {
                throw new UnsupportedOperationException("Not supported with on-demand extensions.");
            }
            return handler.invoke(t, m, a, h);
        };
    }
}

/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
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
package org.skife.jdbi.v3.sqlobject;

import org.skife.jdbi.v3.Transaction;
import org.skife.jdbi.v3.TransactionIsolationLevel;
import org.skife.jdbi.v3.sqlobject.mixins.Transactional;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

class TransactionalHelper
{
    static Map<Method, Handler> handlers()
    {
        try {
            Map<Method, Handler> h = new HashMap<Method, Handler>();
            h.put(Transactional.class.getMethod("begin"), new BeginHandler());
            h.put(Transactional.class.getMethod("commit"), new CommitHandler());
            h.put(Transactional.class.getMethod("rollback"), new RollbackHandler());

            h.put(Transactional.class.getMethod("checkpoint", String.class), new CheckpointHandler());
            h.put(Transactional.class.getMethod("release", String.class), new ReleaseCheckpointHandler());
            h.put(Transactional.class.getMethod("rollback", String.class), new RollbackCheckpointHandler());

            h.put(Transactional.class.getMethod("inTransaction", Transaction.class), new InTransactionHandler());
            h.put(Transactional.class.getMethod("inTransaction", TransactionIsolationLevel.class, Transaction.class), new InTransactionWithIsolationLevelHandler());
            return h;
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("someone wonkered up the bytecode", e);
        }
    }
}

/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.unstable.eod;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface Transactional<SelfType extends Transactional<SelfType>>
{
    public void begin();

    public void commit();

    public void rollback();

    public void checkpoint(String name);

    public void release(String name);

    public void rollback(String name);

    public <ReturnType> ReturnType inTransaction(Transaction<ReturnType, SelfType> func);

    static class BeginHandler implements Handler
    {
        public Object invoke(Handle h, Object target, Object[] args)
        {
            h.begin();
            return null;
        }
    }

    static class CheckpointHandler implements Handler
    {
        public Object invoke(Handle h, Object target, Object[] args)
        {
            h.checkpoint(String.valueOf(args[0]));
            return null;
        }
    }

    static class ReleaseCheckpointHandler implements Handler
    {
        public Object invoke(Handle h, Object target, Object[] args)
        {
            h.release(String.valueOf(args[0]));
            return null;
        }
    }

    static class RollbackCheckpointHandler implements Handler
    {
        public Object invoke(Handle h, Object target, Object[] args)
        {
            h.rollback(String.valueOf(args[0]));
            return null;
        }
    }

    static class CommitHandler implements Handler
    {
        public Object invoke(Handle h, Object target, Object[] args)
        {
            h.commit();
            return null;
        }
    }

    static class RollbackHandler implements Handler
    {
        public Object invoke(Handle h, Object target, Object[] args)
        {
            h.rollback();
            return null;
        }
    }

    static class InTransactionHandler implements Handler
    {
        public Object invoke(Handle h, final Object target, Object[] args)
        {
            final Transaction t = (Transaction) args[0];
            return h.inTransaction(new TransactionCallback() {
                public Object inTransaction(Object conn, TransactionStatus status) throws Exception
                {
                    return t.inTransaction(target, status);
                }
            });
        }
    }

    static class Helper
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
                return h;
            }
            catch (NoSuchMethodException e) {
                throw new IllegalStateException("someone wonkered up the bytecode", e);
            }
        }
    }
}

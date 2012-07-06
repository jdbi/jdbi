package org.skife.jdbi.v2.sqlobject;

import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.sqlobject.mixins.Transactional;

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

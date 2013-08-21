package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionStatus;

class InTransactionHandler implements Handler
{
    public Object invoke(HandleDing h, final Object target, Object[] args, MethodProxy mp)
    {
        h.retain("transaction#implicit");
        try {
            final Transaction t = (Transaction) args[0];
            return h.getHandle().inTransaction(new TransactionCallback()
            {
                public Object inTransaction(Handle conn, TransactionStatus status) throws Exception
                {
                    return t.inTransaction(target, status);
                }
            });
        }
        finally {
            h.release("transaction#implicit");
        }
    }
}

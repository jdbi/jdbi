package org.skife.jdbi.v2.sqlobject;

import net.sf.cglib.proxy.MethodProxy;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;

import java.lang.reflect.Method;

class PassThroughTransactionHandler implements Handler
{
    private final TransactionIsolationLevel isolation;

    PassThroughTransactionHandler(Method m, Transaction tx)
    {
        this.isolation = tx.value();
    }

    @Override
    public Object invoke(HandleDing ding, final Object target, final Object[] args, final MethodProxy mp)
    {
        ding.retain("pass-through-transaction");
        try {
            Handle h = ding.getHandle();
            if (isolation == TransactionIsolationLevel.INVALID_LEVEL) {
                return h.inTransaction(new TransactionCallback<Object>()
                {
                    @Override
                    public Object inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        try {
                            return mp.invokeSuper(target, args);
                        }
                        catch (Throwable throwable) {
                            if (throwable instanceof Exception) {
                                throw (Exception) throwable;
                            }
                            else {
                                throw new RuntimeException(throwable);
                            }
                        }
                    }
                });
            }
            else {
                return h.inTransaction(isolation, new TransactionCallback<Object>()
                {
                    @Override
                    public Object inTransaction(Handle conn, TransactionStatus status) throws Exception
                    {
                        try {
                            return mp.invokeSuper(target, args);
                        }
                        catch (Throwable throwable) {
                            if (throwable instanceof Exception) {
                                throw (Exception) throwable;
                            }
                            else {
                                throw new RuntimeException(throwable);
                            }
                        }
                    }
                });

            }
        }

        finally {
            ding.release("pass-through-transaction");
        }
    }
}

package org.skife.jdbi.v2.tweak.transactions;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;
import org.skife.jdbi.v2.DBITestCase;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.TransactionCallback;
import org.skife.jdbi.v2.TransactionIsolationLevel;
import org.skife.jdbi.v2.TransactionStatus;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.tweak.TransactionRunner;

public class TestSerializableTransactionRunner extends DBITestCase
{
    @Override
    protected TransactionRunner getTransactionRunner()
    {
        return new SerializableTransactionRunner();
    }

    @Test
    public void testEventuallyFails() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(5);
        Handle handle = openHandle();

        try {
            handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new TransactionCallback<Void>() {
                @Override
                public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
                {
                    tries.decrementAndGet();
                    throw new SQLException("serialization", "40001");
                }
            });
        } catch (TransactionFailedException e)
        {
            Assert.assertEquals("40001", ((SQLException) e.getCause()).getSQLState());
        }
        Assert.assertEquals(0, tries.get());
    }

    @Test
    public void testEventuallySucceeds() throws Exception
    {
        final AtomicInteger tries = new AtomicInteger(3);
        Handle handle = openHandle();

        handle.inTransaction(TransactionIsolationLevel.SERIALIZABLE, new TransactionCallback<Void>() {
            @Override
            public Void inTransaction(Handle conn, TransactionStatus status) throws Exception
            {
                if (tries.decrementAndGet() == 0)
                {
                    return null;
                }
                throw new SQLException("serialization", "40001");
            }
        });

        Assert.assertEquals(0, tries.get());
    }
}

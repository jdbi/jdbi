package org.skife.jdbi.v2;

import junit.framework.TestCase;
import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.tweak.TransactionHandler;
import org.skife.jdbi.v2.tweak.transactions.LocalTransactionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;
import java.sql.SQLException;

/**
 * 
 */
public abstract class DBITestCase extends TestCase
{
    private final List<BasicHandle> handles = new ArrayList<BasicHandle>();
    private ExecutorService executor;

    public void setUp() throws Exception
    {
        Tools.start();
        Tools.dropAndCreateSomething();
    }

    public void tearDown() throws Exception
    {
        for (BasicHandle handle : handles)
        {
            handle.close();
        }
        Tools.stop();
    }

    protected BasicHandle openHandle() throws SQLException
    {
        BasicHandle h = new BasicHandle(getTransactionHandler(),
                                        new NamedParameterStatementRewriter(),
                                        Tools.getConnection());
        handles.add(h);
        return h;
    }

    protected TransactionHandler getTransactionHandler()
    {
        return new LocalTransactionHandler();
    }

    protected <T> Future<T> run(Callable<T> it)
    {
        if (this.executor == null) {
            this.executor = Executors.newCachedThreadPool();
        }
        return executor.submit(it);    
    }

}

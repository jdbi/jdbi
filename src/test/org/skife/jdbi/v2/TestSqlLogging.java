package org.skife.jdbi.v2;

import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.logging.Log4JLog;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 *
 */
public class TestSqlLogging extends DBITestCase
{
    private Handle h;
    private List<String> logged;
    private SQLLog log;

    public void setUp() throws Exception
    {
        super.setUp();
        h = openHandle();
        logged = new ArrayList<String>();
        log = new SQLLog()
        {
            public void logBeginTransaction()
            {
                logged.add("begin");
            }

            public void logCommitTransaction()
            {
                logged.add("commit");
            }

            public void logRollbackTransaction()
            {
                logged.add("rollback");
            }

            public void logObtainHandle(Handle h)
            {
                logged.add("open");
            }

            public void logReleaseHandle(Handle h)
            {
                logged.add("close");
            }

            public void logSQL(String sql)
            {
                logged.add(sql);
            }

            public void logPreparedBatch(String sql, int count)
            {
                logged.add(String.format("%d:%s", count, sql));
            }

            public BatchLogger logBatch()
            {
                return new SQLLog.BatchLogger()
                {

                    public void add(String sql)
                    {
                        logged.add(sql);
                    }

                    public void log()
                    {
                    }
                };
            }

            public void logCheckpointTransaction(String name)
            {
                logged.add(String.format("checkpoint %s created", name));
            }

            public void logReleaseCheckpointTransaction(String name)
            {
                logged.add(String.format("checkpoint %s released", name));
            }

            public void logRollbackToCheckpoint(String name)
            {
                logged.add(String.format("checkpoint %s rolled back to", name));
            }
        };
        h.setSQLLog(log);
    }

    public void tearDown() throws Exception
    {
        if (h != null) h.close();
        Tools.stop();
    }

    public void testInsert() throws Exception
    {
        h.insert("insert into something (id, name) values (?, ?)", 1, "Hello");
        assertEquals(1, logged.size());
        assertEquals("insert into something (id, name) values (?, ?)", logged.get(0));
    }

    public void testBatch() throws Exception
    {
        String sql1 = "insert into something (id, name) values (1, 'Eric')";
        String sql2 = "insert into something (id, name) values (2, 'Keith')";
        h.createBatch().add(sql1).add(sql2).execute();
        assertEquals(2, logged.size());
        assertEquals(sql1, logged.get(0));
        assertEquals(sql2, logged.get(1));
    }

    public void testPreparedBatch() throws Exception
    {
        String sql = "insert into something (id, name) values (?, ?)";
        h.prepareBatch(sql).add(1, "Eric").add(2, "Keith").execute();
        assertEquals(1, logged.size());
        assertEquals(String.format("%d:%s", 2, sql), logged.get(0));
    }

    public void testLog4J() throws Exception
    {
        BasicConfigurator.configure(new AppenderSkeleton()
        {

            protected void append(LoggingEvent loggingEvent)
            {
                logged.add(loggingEvent.getRenderedMessage());
            }

            public boolean requiresLayout()
            {
                return false;
            }

            public void close()
            {
            }
        });
        h.setSQLLog(new Log4JLog());
        Logger.getLogger("org.skife.jdbi").setLevel(Level.DEBUG);

        String sql1 = "insert into something (id, name) values (1, 'Eric')";
        String sql2 = "insert into something (id, name) values (2, 'Keith')";
        h.createBatch().add(sql1).add(sql2).execute();
        assertEquals(1, logged.size());
        assertEquals("batch:[[insert into something (id, name) values (1, 'Eric')], [insert into something (id, name) values (2, 'Keith')]]", logged.get(0));
    }

    public void testPrintStream() throws Exception
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        h.setSQLLog(new PrintStreamLog(new PrintStream(bout)));
        String sql = "insert into something (id, name) values (?, ?)";
        h.insert(sql, 1, "Brian");
        assertEquals(String.format("statement:[%s]\n", sql), new String(bout.toByteArray()));
    }

    public void testCloseLogged() throws Exception
    {
        h.close();
        assertTrue(logged.contains("close"));
    }

    public void testLogBegin() throws Exception
    {
        h.begin();
        assertTrue(logged.contains("begin"));
        h.commit();
    }

    public void testLogCommit() throws Exception
    {
        h.begin();
        h.commit();
        assertTrue(logged.contains("commit"));
    }

    public void testLogBeginCommit() throws Exception
    {
        h.inTransaction(new TransactionCallback<Object>()
        {
            public Object inTransaction(Handle handle, TransactionStatus status) throws Exception
            {
                assertTrue(logged.contains("begin"));
                return null;
            }
        });
        assertTrue(logged.contains("commit"));
    }

    public void testLogBeginRollback() throws Exception
    {
        try {
            h.inTransaction(new TransactionCallback<Object>()
            {
                public Object inTransaction(Handle handle, TransactionStatus status) throws Exception
                {
                    assertTrue(logged.contains("begin"));
                    throw new Exception();
                }
            });
            fail("should have raised exception");
        }
        catch (TransactionFailedException e) {
            assertTrue(logged.contains("rollback"));
        }
    }

    public void testLogRollback() throws Exception
    {
        h.begin();
        h.rollback();
        assertTrue(logged.contains("rollback"));
    }

    public void testCheckpoint() throws Exception
    {
        h.begin();

        h.checkpoint("a");
        assertTrue(logged.contains("checkpoint a created"));
        h.rollback("a");
        assertTrue(logged.contains("checkpoint a rolled back to"));

        h.checkpoint("b");
        assertTrue(logged.contains("checkpoint b created"));
        h.release("b");
        assertTrue(logged.contains("checkpoint b released"));

        h.commit();
    }
}

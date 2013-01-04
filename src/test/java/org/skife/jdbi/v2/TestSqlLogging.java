package org.skife.jdbi.v2;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.skife.jdbi.derby.Tools;
import org.skife.jdbi.v2.exceptions.TransactionFailedException;
import org.skife.jdbi.v2.logging.Log4JLog;
import org.skife.jdbi.v2.logging.PrintStreamLog;
import org.skife.jdbi.v2.tweak.SQLLog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TestSqlLogging extends DBITestCase
{
    private Handle h;
    private List<String> logged;
    private SQLLog log;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        h = openHandle();
        logged = new ArrayList<String>();
        log = new SQLLog()
        {
            public void logBeginTransaction(Handle h)
            {
                logged.add("begin");
            }

            public void logCommitTransaction(long time, Handle h)
            {
                logged.add("commit");
            }

            public void logRollbackTransaction(long time, Handle h)
            {
                logged.add("rollback");
            }

            public void logObtainHandle(long time, Handle h)
            {
                logged.add("open");
            }

            public void logReleaseHandle(Handle h)
            {
                logged.add("close");
            }

            public void logSQL(long time, String sql)
            {
                logged.add(sql);
            }

            public void logBinding(int offset, String name, String value) {
                logged.add(String.format("logBinding - %d, %s, %s", offset, name, value));
            }

            public void logPreparedBatch(long time, String sql, int count)
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

                    public void log(long time)
                    {
                    }
                };
            }

            public void logCheckpointTransaction(Handle h, String name)
            {
                logged.add(String.format("checkpoint %s created", name));
            }

            public void logReleaseCheckpointTransaction(Handle h, String name)
            {
                logged.add(String.format("checkpoint %s released", name));
            }

            public void logRollbackToCheckpoint(long time, Handle h, String name)
            {
                logged.add(String.format("checkpoint %s rolled back to", name));
            }
        };
        h.setSQLLog(log);
    }

    @Override
    public void tearDown() throws Exception
    {
        if (h != null) h.close();
        Tools.stop();
    }

    public void testInsert() throws Exception
    {
        h.insert("insert into something (id, name) values (?, ?)", 1, "Hello");
        assertEquals(1 + 2 /*binding logging*/, logged.size());
        assertEquals("logBinding - 1, null, 1", logged.get(0));
        assertEquals("logBinding - 2, null, Hello", logged.get(1));
        assertEquals("insert into something (id, name) values (?, ?)", logged.get(2));
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
        assertEquals(1 + 4 /* binding logging */, logged.size());
        assertEquals("logBinding - 1, null, 1", logged.get(0));
        assertEquals("logBinding - 2, null, Eric", logged.get(1));
        assertEquals("logBinding - 1, null, 2", logged.get(2));
        assertEquals("logBinding - 2, null, Keith", logged.get(3));
        assertEquals(String.format("%d:%s", 2, sql), logged.get(4));
    }

    public void testLog4J() throws Exception
    {
        BasicConfigurator.configure(new AppenderSkeleton()
        {
            @Override
            protected void append(LoggingEvent loggingEvent)
            {
                logged.add(loggingEvent.getRenderedMessage());
            }

            @Override
            public boolean requiresLayout()
            {
                return false;
            }

            @Override
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


        assertTrue(logged.get(0).matches("batch:\\[\\[insert into something \\(id, name\\) values \\(1, 'Eric'\\)\\], \\[insert into something \\(id, name\\) values \\(2, 'Keith'\\)\\]\\] took \\d+ millis"));
    }

    private static final String linesep = System.getProperty("line.separator");

    public void testPrintStream() throws Exception
    {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        h.setSQLLog(new PrintStreamLog(new PrintStream(bout)));
        String sql = "insert into something (id, name) values (?, ?)";
        h.insert(sql, 1, "Brian");

        assertTrue(new String(bout.toByteArray())
                .matches("binding argument offset 1 \\[null\\] to 1" + linesep + "binding argument offset 2 \\[null\\] to Brian" + linesep + "statement:\\[insert into something \\(id, name\\) values \\(\\?, \\?\\)\\] took \\d+ millis" + linesep));
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

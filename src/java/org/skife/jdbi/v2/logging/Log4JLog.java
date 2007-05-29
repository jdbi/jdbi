package org.skife.jdbi.v2.logging;

import org.skife.jdbi.v2.tweak.SQLLog;
import org.skife.jdbi.v2.DBI;
import org.apache.log4j.Logger;

/**
 * Logs SQL to the org.skife.jdbi.v2 logger at the debug level
 */
public class Log4JLog implements SQLLog
{
    private final Logger log = Logger.getLogger(DBI.class.getPackage().getName());

    public void logSQL(String sql)
    {
        if (log.isDebugEnabled()) log.debug(String.format("statement:[%s]", sql));
    }

    public void logPreparedBatch(String sql, int count)
    {
        if (log.isDebugEnabled()) log.debug(String.format("prepared batch with %d parts:[%s]", count, sql));
    }

    public BatchLogger logBatch()
    {
        if (log.isDebugEnabled()) {
            final StringBuilder builder = new StringBuilder();
            builder.append("batch:[");
            return new BatchLogger()
            {
                private boolean added = false;
                public final void add(String sql)
                {
                    added = true;
                    builder.append("[").append(sql).append("], ");
                }

                public final void log()
                {
                    if (added) {
                    builder.delete(builder.length() - 2, builder.length());
                    }
                    builder.append("]");
                    log.debug(builder.toString());
                }
            };
        }
        else {
            return NoOpLog.batch;
        }
    }
}

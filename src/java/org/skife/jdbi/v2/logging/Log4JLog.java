package org.skife.jdbi.v2.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.skife.jdbi.v2.DBI;

/**
 * Logs SQL via Log4J
 */
public final class Log4JLog extends FormattedLog
{
    private final Logger log;
    private Priority level;

    /**
     * Logs to org.skife.jdbi.v2 logger at the debug level
     */
    public Log4JLog()
    {
        this(Logger.getLogger(DBI.class.getPackage().getName()));
    }

    /**
     * Use an arbitrary logger to log to at the debug level
     */
    public Log4JLog(Logger log)
    {
        this(log, Level.DEBUG);
    }

    /**
     * Specify both the logger and the priority to log at
     * @param log The logger to log to
     * @param level the priority to log at
     */
    public Log4JLog(Logger log, Priority level) {
        this.log = log;
        this.level = level;
    }

    protected final boolean isEnabled()
    {
        return log.isEnabledFor(level);
    }

    protected final void log(String msg)
    {
        log.log(level, msg);
    }
}

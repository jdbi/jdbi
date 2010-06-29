package org.skife.jdbi.v2.logging;

import java.io.PrintStream;

/**
 *
 */
public class PrintStreamLog extends FormattedLog
{
    private final PrintStream out;


    /**
     * Log to standard out.
     */
    public PrintStreamLog()
    {
        this(System.out);
    }

    /**
     * Specify the print stream to log to
     * @param out The print stream to log to
     */
    public PrintStreamLog(PrintStream out) {
        this.out = out;
    }

    @Override
    protected final boolean isEnabled()
    {
        return true;
    }

    @Override
    protected void log(String msg)
    {
        synchronized(out) {
            out.println(msg);
        }
    }
}

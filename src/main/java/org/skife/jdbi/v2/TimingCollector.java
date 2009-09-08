package org.skife.jdbi.v2;

/**
 * This class collects timing information for every database operation.
 */
public interface TimingCollector
{
    /**
     * This method is executed every time there is information to collect. Grouping of the
     * timing information is up to the implementation of this interface.
     *
     * @param sql The rewritten SQL that was executed.
     * @param ctx The Statement Context.
     * @param elapsedTime The elapsed time in nanoseconds.
     */
    void collect(String sql, StatementContext ctx, long elapsedTime);

    /**
     * A No Operation Timing Collector. It can be used to "plug" into DBI if more sophisticated
     * collection is not needed.
     */
    TimingCollector NOP_TIMING_COLLECTOR = new NopTimingCollector();

    public static final class NopTimingCollector implements TimingCollector
    {
        public void collect(final String sql, final StatementContext ctx, final long elapsedTime)
        {
            // GNDN
        }
    };
}

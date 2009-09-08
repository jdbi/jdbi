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
     * @param ctx The Statement Context, which contains additional information about the
     * statement that just ran.
     * @param elapsedTime The elapsed time in nanoseconds.
     */
    void collect(long elapsedTime, StatementContext ctx);

    /**
     * A No Operation Timing Collector. It can be used to "plug" into DBI if more sophisticated
     * collection is not needed.
     */
    TimingCollector NOP_TIMING_COLLECTOR = new NopTimingCollector();

    public static final class NopTimingCollector implements TimingCollector
    {
        public void collect(final long elapsedTime, final StatementContext ctx)
        {
            // GNDN
        }
    };
}

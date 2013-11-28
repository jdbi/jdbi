/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v3;

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

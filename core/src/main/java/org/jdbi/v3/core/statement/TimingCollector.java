/*
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
package org.jdbi.v3.core.statement;

/**
 * This class collects timing information for statement execution.
 */
@FunctionalInterface
public interface TimingCollector {
    /**
     * Called after a statement completes with how long it took to execute.
     *
     * @param elapsedNs the elapsed time in nanoseconds.
     * @param ctx the context of the just completed statement
     */
    void collect(long elapsedNs, StatementContext ctx);

    /**
     * GNDN.
     */
    TimingCollector NOP_TIMING_COLLECTOR = (ns, ctx) -> {};
}

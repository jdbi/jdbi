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
package org.jdbi.v3.core.config.cache;

import java.util.Set;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleListener;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

public final class JdbiConfigCacheStatsReporter implements StatementContextListener, HandleListener {

    public static final JdbiConfigCacheStatsReporter INSTANCE = new JdbiConfigCacheStatsReporter();

    private static final Logger LOG = LoggerFactory.getLogger(JdbiConfigCacheStatsReporter.class);

    @Override
    public void handleClosed(Handle handle) {
        Set<JdbiConfigCacheStats> cacheStats = handle.getConfig().reportStats();
        report("Handle", cacheStats);
    }

    @Override
    public void contextCleaned(StatementContext statementContext) {
        Set<JdbiConfigCacheStats> cacheStats = statementContext.getConfig().reportStats();
        report("Context", cacheStats);
    }

    private void report(String context, Set<JdbiConfigCacheStats> stats) {
        LOG.info("------------------------------------------------------------------------");
        LOG.info(format("Cache Stats for %s", context));
        stats.forEach(s -> LOG.info(s.toString()));
    }
}

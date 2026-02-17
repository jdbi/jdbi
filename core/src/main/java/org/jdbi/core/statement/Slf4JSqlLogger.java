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
package org.jdbi.core.statement;

import java.sql.SQLException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Simple {@link SqlLogger} that emits some diagnostic information about
 * Jdbi usage. Usually you shouldn't log every database statement made but
 * it can be helpful for debugging.
 */
public class Slf4JSqlLogger implements SqlLogger {
    private final Logger log;
    private final Level level;

    public Slf4JSqlLogger() {
        this(LoggerFactory.getLogger("org.jdbi.sql"), Level.DEBUG);
    }

    public Slf4JSqlLogger(Logger log) {
        this(log, Level.DEBUG);
    }

    public Slf4JSqlLogger(Level level) {
        this(LoggerFactory.getLogger("org.jdbi.sql"), level);
    }

    public Slf4JSqlLogger(Logger log, Level level) {
        this.log = log;
        this.level = level;
    }

    @Override
    public void logAfterExecution(StatementContext context) {
        if (log.isEnabledForLevel(this.level)) {
            log.atLevel(this.level).log("Executed in {} '{}' with parameters '{}'",
                    format(Duration.between(context.getExecutionMoment(), context.getCompletionMoment())),
                    getSql(context),
                    context.getBinding());
        }
    }

    @Override
    public void logException(StatementContext context, SQLException ex) {
        if (log.isErrorEnabled()) {
            log.error("Exception while executing '{}' with parameters '{}'",
                getSql(context),
                context.getBinding(),
                ex);
        }
    }

    private static String getSql(StatementContext context) {
        ParsedSql parsedSql = context.getParsedSql();
        if (parsedSql != null) {
            return parsedSql.getSql();
        }
        return "<not available>";
    }

    private static String format(Duration duration) {
        final long totalSeconds = duration.toSeconds();
        final long h = totalSeconds / 3600;
        final long m = (totalSeconds % 3600) / 60;
        final long s = totalSeconds % 60;
        final long ms = duration.toMillis() % 1000;
        return String.format(
                "%d:%02d:%02d.%03d",
                h, m, s, ms);
    }
}

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
package org.skife.jdbi.v3.logging;

import org.skife.jdbi.v3.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JLog extends FormattedLog {

    public static enum Level
    {
        TRACE {
            @Override
            boolean isEnabled(Logger logger) {
                return logger.isTraceEnabled();
            }

            @Override
            void log(Logger logger, String msg) {
                logger.trace(msg);
            }
        },
        DEBUG {
            @Override
            boolean isEnabled(Logger logger) {
                return logger.isDebugEnabled();
            }

            @Override
            void log(Logger logger, String msg)
            {
                logger.debug(msg);
            }
        },
        INFO {
            @Override
            boolean isEnabled(Logger logger) {
                return logger.isInfoEnabled();
            }

            @Override
            void log(Logger logger, String msg) {
                logger.info(msg);
            }
        },
        WARN {
            @Override
            boolean isEnabled(Logger logger) {
                return logger.isWarnEnabled();
            }

            @Override
            void log(Logger logger, String msg) {
                logger.warn(msg);
            }
        },
        ERROR {
            @Override
            boolean isEnabled(Logger logger) {
                return logger.isErrorEnabled();
            }

            @Override
            void log(Logger logger, String msg) {
                logger.error(msg);
            }
        };


        abstract boolean isEnabled(Logger logger);
        abstract void log(Logger logger, String msg);
    }

    private final Logger logger;
    private final Level level;

    public SLF4JLog() {
        this(LoggerFactory.getLogger(DBI.class.getPackage().getName()));
    }

    public SLF4JLog(Logger logger) {
        this(logger, Level.TRACE);
    }

    public SLF4JLog(Logger logger, Level level) {
        this.logger = logger;
        this.level = level;
    }

    @Override
    protected boolean isEnabled() {
        return level.isEnabled(logger);
    }

    @Override
    protected void log(String msg) {
        level.log(logger, msg);
    }
}

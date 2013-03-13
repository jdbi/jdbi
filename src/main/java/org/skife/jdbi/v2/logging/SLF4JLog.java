/*
 * Copyright 2004 - 2011 Brian McCallister
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

package org.skife.jdbi.v2.logging;

import org.apache.log4j.Level;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SLF4JLog extends FormattedLog {
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
        if (level == Level.ERROR) {
            return logger.isErrorEnabled();
        }
        if (level == Level.WARN) {
            return logger.isWarnEnabled();
        }
        if (level == Level.INFO) {
            return logger.isInfoEnabled();
        }
        if (level == Level.DEBUG) {
            return logger.isDebugEnabled();
        }
        if (level == Level.TRACE) {
            return logger.isTraceEnabled();
        }
        return false;
    }

    @Override
    protected void log(String msg) {
        if (level == Level.ERROR) {
            logger.error(msg);
        } else if (level == Level.WARN) {
            logger.warn(msg);
        } else if (level == Level.INFO) {
            logger.info(msg);
        } else if (level == Level.DEBUG) {
            logger.debug(msg);
        } else if (level == Level.TRACE) {
            logger.trace(msg);
        }
    }
}

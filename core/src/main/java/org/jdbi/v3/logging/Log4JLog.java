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
package org.jdbi.v3.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.jdbi.v3.DBI;

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

    @Override
    protected boolean isEnabled()
    {
        return log.isEnabledFor(level);
    }

    @Override
    protected void log(String msg)
    {
        log.log(level, msg);
    }
}

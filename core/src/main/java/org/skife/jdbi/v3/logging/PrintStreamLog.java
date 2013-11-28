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

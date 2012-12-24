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

package org.skife.jdbi.v2;

import org.skife.jdbi.v2.tweak.StatementLocator;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Represents a number of SQL statements which will be executed in a batch statement.
 */
public class Script
{

    private static final Pattern WHITESPACE_ONLY = Pattern.compile("^\\s*$");

    private final Handle handle;
    private final StatementLocator locator;
    private final String name;
    private final Map<String, Object> globalStatementAttributes;

    Script(Handle h, StatementLocator locator, String name, Map<String, Object> globalStatementAttributes)
    {
        this.handle = h;
        this.locator = locator;
        this.name = name;
        this.globalStatementAttributes = globalStatementAttributes;
    }

    /**
     * Execute this script in a batch statement
     *
     * @return an array of ints which are the results of each statement in the script
     */
    public int[] execute()
    {
        final String script;
        final StatementContext ctx = new ConcreteStatementContext(handle, globalStatementAttributes);
        try
        {
            script = locator.locate(name, ctx);
        }
        catch (Exception e)
        {
            throw handle.getExceptionPolicy().unableToExecuteStatement(String.format("Error while loading script [%s]", name), e, ctx);
        }

        final String[] statements = script.replaceAll("\n", " ").replaceAll("\r", "").split(";");
        Batch b = handle.createBatch();
        for (String s : statements)
        {
            if ( ! WHITESPACE_ONLY.matcher(s).matches() ) {
                b.add(s);
            }
        }
        return b.execute();
    }
}

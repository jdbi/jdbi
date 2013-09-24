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
package org.skife.jdbi.v2.tweak;

import org.skife.jdbi.v2.StatementContext;

/**
 * Used for finding the actual SQL for named statements..
 */
public interface StatementLocator
{
    /**
     * Use this to map from a named statement to SQL. The SQL returned will be passed to
     * a StatementRewriter, so this can include stuff like named params and whatnot.
     *
     * @param name The name of the statement, as provided to a Handle
     * @return the SQL to execute, after it goes through a StatementRewriter
     * @throws Exception if anything goes wrong, jDBI will percolate expected exceptions
     */
    public String locate(String name, StatementContext ctx) throws Exception;
}

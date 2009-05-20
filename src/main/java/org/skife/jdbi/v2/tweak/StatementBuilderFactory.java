/*
 * Copyright 2004-2007 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.skife.jdbi.v2.tweak;

import java.sql.Connection;

/**
 * Used to specify how prepared statements are built. A factry is attached to a DBI instance, and
 * whenever the DBI instance is used to create a Handle the factory will be used to create a
 * StatementBuilder for that specific handle.
 * <p>
 * The default implementation caches all prepared statements for a given Handle instance. To change
 * the factory, use {@link org.skife.jdbi.v2.DBI#setStatementBuilderFactory(StatementBuilderFactory)}.
 */
public interface StatementBuilderFactory
{
    /**
     * Obtain a StatementBuilder, called when a new handle is opened
     */
    StatementBuilder createStatementBuilder(Connection conn);
}

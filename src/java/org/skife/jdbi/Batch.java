/* Copyright 2004-2005 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi;

import java.util.Collection;

/**
 * Represents a batch of arbitrary statements to be executed together, see
 * JDBC batched statements
 */
public interface Batch
{
    /**
     * A statement to be executed as part of the batch
     *
     * @param statement direct sql, no parameters, no named statements
     */
    Batch add(String statement);

    /**
     * Add statements en masse
     *
     * @param statements Collection<String> of sql statements
     */
    Batch addAll(Collection statements);

    /**
     * Execute all of the statements in this batch and clear the batch
     *
     * @return an array of the number of rows modified in each statement
     */
    int[] execute() throws DBIException;
}

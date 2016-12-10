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
package org.jdbi.v3.core;

import java.sql.PreparedStatement;

/**
 * Holds a {@code PreparedStatement}, determines execution strategy,
 * and then produces a result.  Used to implement core and custom statement
 * execution strategies.  Most users will not use this class directly.
 *
 * @see ResultIterable for the more commonly invoked interface
 */
public interface ResultBearing
{
    /**
     * Execute the statement.  The given {@code ResultProducer}
     * gets the post-execution {@link PreparedStatement} and constructs
     * the value to return.
     *
     * @param producer given the executed statement, produce results
     * @return the produced results
     */
    <R> R execute(ResultProducer<R> producer);
}

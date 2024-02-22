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

import org.jdbi.core.result.ResultProducer;

public interface QueryExecute {

    /**
     * Executes the query, returning the result obtained from the given {@link ResultProducer}.
     *
     * @param <R> the type of the result
     * @param producer the result producer.
     * @return value returned by the result producer.
     */
    <R> R execute(ResultProducer<R> producer);

    /**
     * Specify that the result set should be concurrent updatable.
     *
     * This will allow the update methods to be called on the result set produced by this
     * Query.
     *
     * @return the modified query
     */
    Query concurrentUpdatable();
}

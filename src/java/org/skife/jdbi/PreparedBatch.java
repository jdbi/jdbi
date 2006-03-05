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

import java.util.Map;
import java.util.Collection;

/**
 * Represents a JDBC batch statement of the prepared variety. The sql must
 * be the same for each "statement" but different parameters are bound to
 * each.
 */
public interface PreparedBatch
{
    /**
     * Add a batched statement execution with positional arguments
     */
    PreparedBatch add(Object[] objects);

    /**
     * Collection containing arguments to bindBinaryStream positionally
     */
    PreparedBatch add(Collection params);

    /**
     * Execute the batch returning an array of the number of rows modified
     */
    int[] execute() throws DBIException;

    /**
     * Map properties on <code>bean</code> to named parameters on the statement
     *
     * @param bean a JavaBean
     */
    PreparedBatch add(Object bean);

    /**
     * Convenience method for adding a large number of statements to a batch.
     * Contents may be either Map, Object[], a JavaBean, or a Collection.
     */
    PreparedBatch addAll(Collection args);

    /**
     * Convenience method for adding a large number of statements to a batch.
     * Contents may be either Map, Object[], a JavaBean, or a Collection.
     */
    PreparedBatch addAll(Object[] args);

    /**
     * Populate named parameters via values in a map
     *
     * @param params String -> Value keyed map
     */
    PreparedBatch add(Map params);
}

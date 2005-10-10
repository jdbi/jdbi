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

import java.util.Iterator;
import java.util.ListIterator;

/**
 * An abstract interface representing an SQL query. This can be
 * used to build up queries with various options.
 */
public interface Query
{
    /**
     * Will execute the query and return an Iterator which contains Map
     * instances. This is a lazy iterator, only instantiating maps and reading
     * from the resultset as the next element is requested.
     *
     * @return an iterator over the results
     */
    Iterator iterator();

    /**
     * Close resources associated with this query
     */
    void close();

    /**
     * Return a ListIterator which can traverse the result set backwards and forwards
     * @return a ListIterator over the results
     */
    ListIterator listIterator();
}

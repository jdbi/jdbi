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

/**
 * Callback interface for use with queries. Each row will be passed into
 * the <code>eachRow</code> method in the order returned by the query.
 */
public interface RowCallback
{
    /**
     * Will be called for each row returned by a query. Throwing and
     * exception will abort the result set traversal, rollback the
     * transaction, and cause the calling method to throw an exception
     * wrapping the exception thrown by this method.
     *
     * @param handle database handle query was issued to
     * @param row an individual row from the resultset
     */
    void eachRow(Handle handle, Map row) throws Exception;
}

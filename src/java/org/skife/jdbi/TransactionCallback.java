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

/**
 * Callback interface for use with <code>Handle</code> instances. Will ensure everything
 * executed within the callback method is in a single transactions, and will rollback
 * and rethrow if any exception is thrown from the callback
 */
public interface TransactionCallback
{
    /**
     * Called within context of a transaction
     *
     * @param handle will be open and transactional
     * @throws Exception will cause transaction to rollback and be rethrown
     */
    void inTransaction(Handle handle) throws Exception;
}

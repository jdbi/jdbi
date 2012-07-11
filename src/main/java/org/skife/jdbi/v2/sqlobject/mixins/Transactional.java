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

package org.skife.jdbi.v2.sqlobject.mixins;

import org.skife.jdbi.v2.Transaction;
import org.skife.jdbi.v2.TransactionIsolationLevel;

/**
 * A mixin interface to expose transaction methods on the sql object.
 *
 * @param <SelfType> must match the interface that is extending this one.
 */
public interface Transactional<SelfType extends Transactional<SelfType>>
{
    public void begin();

    public void commit();

    public void rollback();

    public void checkpoint(String name);

    public void release(String name);

    public void rollback(String name);

    public <ReturnType> ReturnType inTransaction(Transaction<ReturnType, SelfType> func);

    public <ReturnType> ReturnType inTransaction(TransactionIsolationLevel isolation, Transaction<ReturnType, SelfType> func);
}

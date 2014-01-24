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
    void begin();

    void commit();

    void rollback();

    void checkpoint(String name);

    void release(String name);

    void rollback(String name);

    <ReturnType> ReturnType inTransaction(Transaction<ReturnType, SelfType> func);

    <ReturnType> ReturnType inTransaction(TransactionIsolationLevel isolation, Transaction<ReturnType, SelfType> func);
}

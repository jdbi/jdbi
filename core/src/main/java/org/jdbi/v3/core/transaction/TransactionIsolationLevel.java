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
package org.jdbi.v3.core.transaction;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Supported transaction isolation levels.
 */
public final class TransactionIsolationLevel {
    private static final Map<Integer, TransactionIsolationLevel> VALUE_OF = new ConcurrentHashMap<>();
    public static final TransactionIsolationLevel NONE =
            valueOf(Connection.TRANSACTION_NONE, "NONE");
    public static final TransactionIsolationLevel READ_UNCOMMITTED =
            valueOf(Connection.TRANSACTION_READ_UNCOMMITTED, "READ_UNCOMMITTED");
    public static final TransactionIsolationLevel READ_COMMITTED =
            valueOf(Connection.TRANSACTION_READ_COMMITTED, "READ_COMMITTED");
    public static final TransactionIsolationLevel REPEATABLE_READ =
            valueOf(Connection.TRANSACTION_REPEATABLE_READ, "REPEATABLE_READ");
    public static final TransactionIsolationLevel SERIALIZABLE =
            valueOf(Connection.TRANSACTION_SERIALIZABLE, "SERIALIZABLE");
    /** The transaction isolation level wasn't specified or is unknown to jdbi. */
    public static final TransactionIsolationLevel UNKNOWN =
            valueOf(Integer.MIN_VALUE, "UNKNOWN");

    private final String name;
    private final int value;

    private TransactionIsolationLevel(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public int intValue() {
        return this.value;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static TransactionIsolationLevel valueOf(int val) {
        return valueOf(val, "UNKNOWN");
    }

    public static TransactionIsolationLevel valueOf(int val, String name) {
        return VALUE_OF.computeIfAbsent(val, x -> new TransactionIsolationLevel(name, val));
    }
}

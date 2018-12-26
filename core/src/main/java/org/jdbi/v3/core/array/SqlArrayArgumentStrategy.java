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
package org.jdbi.v3.core.array;

/**
 * Strategies used to bind SQL array arguments to a {@link java.sql.PreparedStatement}.
 */
public enum SqlArrayArgumentStrategy {
    /**
     * Bind using {@link java.sql.Connection#createArrayOf(String, Object[])} and
     * {@link java.sql.PreparedStatement#setArray(int, java.sql.Array)}.
     */
    SQL_ARRAY,

    /**
     * Bind using {@link java.sql.PreparedStatement#setObject(int, Object)} with an array object.
     */
    OBJECT_ARRAY
}

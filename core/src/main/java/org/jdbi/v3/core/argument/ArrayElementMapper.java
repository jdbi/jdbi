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
package org.jdbi.v3.core.argument;

/**
 * Maps array elements into SQL array elements compatible with a specific JDBC driver.
 * @param <T> the element type being mapped/converted.
 */
public interface ArrayElementMapper<T> {
    /**
     * Returns the type name {@code String} for the given element type. This value is suitable for passing to
     * {@link java.sql.Connection#createArrayOf(String, Object[])}.
     */
    String getTypeName();

    /**
     * Maps {@code T} objects into types supported in {@link java.sql.Array SQL arrays} by a given JDBC driver.
     * Maps Returns an equivalent object, in a array element type supported by a specific driver for SQL arrays
     * @param element
     * @return
     */
    Object mapArrayElement(T element);
}

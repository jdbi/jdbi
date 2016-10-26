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
 * Strategy for converting array-like arguments into SQL arrays.
 *
 * @param <T> the array element type.
 */
public interface SqlArrayType<T> {
    /**
     * Returns the vendor-specific SQL type name {@code String} for the element type {@code T}. This value will be
     * passed to {@link java.sql.Connection#createArrayOf(String, Object[])} to create SQL arrays.
     */
    String getTypeName();

    /**
     * Returns an equivalent value in a type supported by the JDBC vendor. If element type {@code T} is already
     * supported by the JDBC vendor, this method may return {@code element} without modification.
     *
     * @param element the element to convert.
     * @return the converted element.
     */
    Object convertArrayElement(T element);
}

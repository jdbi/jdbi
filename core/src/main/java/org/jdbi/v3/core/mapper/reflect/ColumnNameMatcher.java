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
package org.jdbi.v3.core.mapper.reflect;

/**
 * Strategy for matching SQL column names to Java property, field, or parameter names.
 */
public interface ColumnNameMatcher {
    /**
     * Returns whether the column name fits the given Java identifier name.
     *
     * @param columnName the SQL column name
     * @param javaName   the Java property, field, or parameter name
     * @return whether the given names are logically equivalent
     */
    boolean columnNameMatches(String columnName, String javaName);
}

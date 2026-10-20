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
package org.jdbi.v3.core.mapper.reflect.internal;

import java.util.List;

import static java.lang.String.format;

/**
 * Explains why a reflective row mapper matched no columns at all. Prefixed mappers are the common trap:
 * the prefix is matched against the column label, so {@code SELECT t.*} yields unprefixed labels that
 * the mapper cannot see.
 */
public final class UnmatchedColumnsHint {
    private UnmatchedColumnsHint() {}

    /**
     * Builds the tail of a "no matching columns" message.
     *
     * @param prefix      the column name prefix of the mapper, empty for none
     * @param columnNames the column labels present in the result set
     * @return a sentence fragment that starts with a space and ends with a period
     */
    public static String forColumns(String prefix, List<String> columnNames) {
        if (prefix.isEmpty()) {
            return format(" Result set columns: %s.", columnNames);
        }
        return format(" Result set columns: %s. The mapper uses prefix '%2$s' and only matches columns whose label"
                + " starts with it. Alias each selected column with the prefix, for example \"%2$s.id AS %2$s_id\""
                + " instead of \"%2$s.*\".",
            columnNames, prefix);
    }
}

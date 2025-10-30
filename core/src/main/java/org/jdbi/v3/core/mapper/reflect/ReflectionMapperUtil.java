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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.jdbi.v3.core.internal.UtilityClassException;
import org.jdbi.v3.core.mapper.CaseStrategy;

/**
 * Utilities for reflective mappers. This is an internal helper class that should have been package private but as the mappers are spread across the reflect and
 * reflect.internal package, they are not. Any method in here may change at any time.
 */
public final class ReflectionMapperUtil {
    private ReflectionMapperUtil() {
        throw new UtilityClassException();
    }

    /**
     * Returns the name of all the columns present in the specified {@link ResultSet}. The caseStrategy operator is applied to all column names to
     * align upper/lower case etc.
     *
     * @param rs           the {@link ResultSet} to get the column names of
     * @param caseStrategy Apply a specific case strategy to the column names.
     * @return list of all the column names in {@code rs} (will contain duplicates if multiple columns have the same name)
     * @throws SQLException See {@link ResultSet#getMetaData()}, {@link ResultSetMetaData#getColumnCount()}, and {@link ResultSetMetaData#getColumnLabel(int)}
     * @see CaseStrategy
     */
    public static List<String> getColumnNames(ResultSet rs, UnaryOperator<String> caseStrategy) throws SQLException {

        final ResultSetMetaData metadata = rs.getMetaData();
        final int count = metadata.getColumnCount();
        final List<String> columnNames = new ArrayList<>(count);

        for (int i = 0; i < count; ++i) {
            columnNames.add(caseStrategy.apply(metadata.getColumnLabel(i + 1)));
        }

        return columnNames;
    }

    /**
     * Attempts to find the index of a specified column's mapped parameter in a list of column names
     *
     * @param paramName          the name of the parameter to search for
     * @param columnNames        list of column names to search in
     * @param columnNameMatchers {@link ColumnNameMatcher}s to map {@code paramName} to the column names
     * @param debugName          name of the parameter to use for debugging purposes (ie: when throwing exceptions)
     * @return {@link OptionalInt} with the found index, {@link OptionalInt#empty()} otherwise.
     */
    public static OptionalInt findColumnIndex(String paramName, List<String> columnNames, List<ColumnNameMatcher> columnNameMatchers, Supplier<String> debugName) {
        OptionalInt result = OptionalInt.empty();

        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);

            for (ColumnNameMatcher strategy : columnNameMatchers) {
                if (strategy.columnNameMatches(columnName, paramName)) {
                    if (result.isPresent()) {
                        throw new IllegalArgumentException(String.format(
                            "'%s' (%s) matches multiple columns: '%s' (%d) and '%s' (%d)",
                            debugName.get(), paramName,
                            columnNames.get(result.getAsInt()), result.getAsInt(),
                            columnNames.get(i), i));
                    }

                    result = OptionalInt.of(i);
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Returns whether any of the given column names begin with the given prefix, according to the list of column name
     * matchers.
     *
     * @param columnNames the column names to search
     * @param prefix the prefix to search for
     * @param columnNameMatchers list of column name matchers
     * @return whether any of the column names begin with the prefix
     * @since 3.5.0
     */
    public static boolean anyColumnsStartWithPrefix(Collection<String> columnNames, String prefix, List<ColumnNameMatcher> columnNameMatchers) {
        return columnNames.stream().anyMatch(
            columnName -> columnNameMatchers.stream().anyMatch(
                matcher -> matcher.columnNameStartsWith(columnName, prefix)));
    }

    /**
     * Add a prefix separated with '.' to a java bean property name. If the prefix is empty, return the name itself.
     * @param prefix The prefix to add to the property name. The empty string is ignored.
     * @param name A property name.
     * @return The prefixed name.
     */
    public static String addPropertyNamePrefix(String prefix, String name) {
        if (prefix.isEmpty()) {
            return name;
        } else {
            return prefix + "." + name;
        }
    }
}

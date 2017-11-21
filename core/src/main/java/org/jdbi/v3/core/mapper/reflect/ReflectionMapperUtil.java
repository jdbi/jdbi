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
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Supplier;

import org.jdbi.v3.core.statement.StatementContext;

class ReflectionMapperUtil {
  static List<String> getColumnNames(ResultSet rs) throws SQLException {
    final ResultSetMetaData metadata = rs.getMetaData();
    final int count = metadata.getColumnCount();
    final List<String> columnNames = new ArrayList<>(count);

    for (int i = 0; i < count; ++i) {
      columnNames.add(metadata.getColumnLabel(i + 1).toLowerCase());
    }

    return columnNames;
  }

  static List<ColumnNameMatcher> getColumnNameMatchers(StatementContext ctx) {
    return ctx.getConfig(ReflectionMappers.class).getColumnNameMatchers();
  }

  static OptionalInt findColumnIndex(String paramName,
                                     List<String> columnNames,
                                     List<ColumnNameMatcher> columnNameMatchers,
                                     Supplier<String> debugName) {
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
}

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
 * Matches snake case column names to java camel case names, ignoring case.
 * <p>
 * Example: column names {@code first_name} or {@code FIRST_NAME} would match java name {@code firstName}.
 */
public class SnakeCaseColumnNameMatcher implements ColumnNameMatcher {
    @Override
    public boolean columnNameMatches(String columnName, String javaName) {
        return removeUnderscores(columnName).equalsIgnoreCase(removeUnderscores(javaName));
    }

    @Override
    public boolean columnNameStartsWith(String columnName, String prefix) {
        String normalizedPrefix = removeUnderscores(prefix);
        return removeUnderscores(columnName).regionMatches(true, 0, normalizedPrefix, 0, normalizedPrefix.length());
    }

    private String removeUnderscores(String string) {
        return string.replace("_", "");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

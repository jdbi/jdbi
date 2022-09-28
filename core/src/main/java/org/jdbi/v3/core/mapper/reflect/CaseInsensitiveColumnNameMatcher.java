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
 * Matches column names with identical java names, ignoring case.
 * <p>
 * Example: column names {@code firstname} or {@code FIRSTNAME} would match java name {@code firstName}.
 */
public final class CaseInsensitiveColumnNameMatcher implements ColumnNameMatcher {
    @Override
    public boolean columnNameMatches(String columnName, String propertyName) {
        return doMatch(columnName, propertyName, false);
    }

    @Override
    public boolean columnNameStartsWith(String columnName, String prefix) {
        return doMatch(columnName, prefix, true);
    }

    private boolean doMatch(String columnName, String propertyName, boolean columnNameMayTrailCharacters) {
        int cPos = 0;
        int pPos = 0;

        for (;;) {
            boolean cEnded = cPos >= columnName.length();
            boolean pEnded = pPos >= propertyName.length();

            if (cEnded || pEnded) {
                // javaname must have ended. (prefix match give only prefix)
                // column name must end for full match but may trail characters for
                // prefix matches.
                return pEnded && (cEnded || columnNameMayTrailCharacters);
            }

            char pChar = propertyName.charAt(pPos);
            char cChar = columnName.charAt(cPos);

            // skip all prefix separators. This is a pure "column name to java bean name" match where
            // all prefixes etc. are resolved down to a single bean property name.
            if (pChar == '.') {
                pPos++;
            } else {
                if (Character.toLowerCase(cChar) != Character.toLowerCase(pChar)) {
                    return false;
                }
                pPos++;
                cPos++;
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}

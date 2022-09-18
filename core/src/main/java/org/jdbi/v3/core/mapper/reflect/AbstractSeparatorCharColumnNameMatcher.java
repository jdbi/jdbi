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

public abstract class AbstractSeparatorCharColumnNameMatcher implements ColumnNameMatcher {

    private final char separatorChar;

    protected AbstractSeparatorCharColumnNameMatcher(char separatorChar) {
        this.separatorChar = separatorChar;
    }

    @Override
    public boolean columnNameMatches(String columnName, String propertyName) {
        return doMatch(columnName, propertyName, false);
    }

    @Override
    public boolean columnNameStartsWith(String columnName, String prefix) {
        int dotIndex = prefix.lastIndexOf('.');
        String propertyName = dotIndex == -1 ? prefix : prefix.substring(0, dotIndex);
        return doMatch(columnName, propertyName, true);
    }

    private boolean doMatch(String columnName, String propertyName, boolean columnNameMayTrailCharacters) {
        int cPos = 0;
        int pPos = 0;

        boolean separatorMatched = false;
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

            // treat a separator char in the property name the same way as the "correct" separator ('.')
            // This covers the case where a bean gets e.g. an explicit "foo_" prefix (instead of just "foo").
            if (pChar == '.' || pChar == separatorChar) {
                pPos++;
                // found a separator. See if it lines up with a column name separator
                if (cChar == separatorChar) {
                    // yes, it does. Skip that as well.
                    cPos++;
                    separatorMatched = true;
                // if this is a continuation match (e.g. using a prefix with trailing '.'), skip
                // any additional characters. otherwise, this is now a prefix that does not match.
                } else if (!separatorMatched) {
                    return false;
                }
            } else {
                separatorMatched = false;
                // See if the column name sits on a separator. If yes, skip it, then does case insensitive match.
                if (cChar != separatorChar || !Character.isLetterOrDigit(pChar)) {
                    if (Character.toLowerCase(cChar) != Character.toLowerCase(pChar)) {
                        return false;
                    }
                    pPos++;
                }
                cPos++;
            }
        }
    }
}

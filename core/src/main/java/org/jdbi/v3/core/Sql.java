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
package org.jdbi.v3.core;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 *   An immutable sql statement string created from multiple tokens
 *   in order to write inline sql statements in an easy-to-read fashion
 *   spread out over multiple lines of code.
 * </p>
 *
 * <p>
 *   The class implements {@link CharSequence} and thus can be used as a drop-in
 *   alternative wherever API supports {@code CharSequence} rather than {@code String}.
 * </p>
 *
 * Please note that the validity of the statement is never checked,
 * and that {@code null} or empty inputs are permitted (no run-time exceptions).<br>
 *
 * The input of multiple tokens is formatted into a single String by
 * removing leading and trailing whitespace and concatenating
 * non-empty tokens by a single space character.
 * Further, any trailing semicolons are removed from the resulting sql string.
 *
 * <p>Example:</p>
 *
 * <pre>
 *     String tblName = "table";
 *     Sql.of("SELECT COUNT(*)",
 *            "FROM", tblName,
 *            " WHERE cond1 = :cond1",
 *            "   AND cond2 = :cond2");
 * </pre>
 *
 * @author Markus Spann
 */
public final class Sql implements CharSequence {

    /** The internal sql string. Cannot be null. */
    private final String str;

    private Sql(String sql) {
        str = sql;
    }

    public static Sql of(CharSequence... tokens) {
        return tokens == null ? new Sql("") : of(Arrays.asList(tokens));
    }

    public static Sql of(Collection<CharSequence> tokens) {
        return tokens == null ? new Sql("") : new Sql(format(tokens));
    }

    /**
     * Formats an sql statement from multiple tokens.<br>
     * Leading and trailing whitespace is removed from each token and empty tokens ignored.<br>
     * The tokens are joined using a single blank character to create the sql string.<br>
     * Finally, any trailing semicolons are removed from the resulting sql.
     *
     * @param tokens collection of tokens
     * @return formatted sql string
     */
    static String format(Collection<CharSequence> tokens) {
        String sql = Objects.requireNonNull(tokens).stream()
                .filter(Objects::nonNull)
                .map(CharSequence::toString)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "));
        while (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1);
        }
        return sql;
    }

    @Override
    public int length() {
        return str.length();
    }

    @Override
    public char charAt(int index) {
        return str.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return str.subSequence(start, end);
    }

    @Override
    public int hashCode() {
        return str.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return str.equals(((Sql) obj).str);
    }

    @Override
    public String toString() {
        return str;
    }

}

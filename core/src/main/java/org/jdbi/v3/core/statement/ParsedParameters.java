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
package org.jdbi.v3.core.statement;

import static java.util.Collections.unmodifiableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The parsed parameters from an SQL statement.
 */
public class ParsedParameters {
    private final boolean positional;
    private final List<String> parameterNames;

    ParsedParameters(boolean positional, List<String> parameterNames) {
        this.positional = positional;
        this.parameterNames = unmodifiableList(new ArrayList<>(parameterNames));
    }

    /**
     * @return true if the SQL statement uses positional parameters, false if
     * the statement uses named parameters, or has no parameters at all.
     */
    public boolean isPositional() {
        return positional;
    }

    /**
     * @return the number of parameters from the SQL statement.
     */
    public int getParameterCount() {
        return parameterNames.size();
    }

    /**
     * @return the parameter names from the SQL statement.
     */
    public List<String> getParameterNames() {
        return parameterNames;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParsedParameters that = (ParsedParameters) o;
        return positional == that.positional
            && Objects.equals(parameterNames, that.parameterNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(positional, parameterNames);
    }

    @Override
    public String toString() {
        return "ParsedParameters{"
            + "positional=" + positional
            + ", parameterNames=" + parameterNames
            + '}';
    }

    /**
     * A static factory of named {@link ParsedParameters} instances.
     * List given in argument must contain parameter names from the
     * related statement. They must be bare names, free of SQL
     * variable processing syntax such as a prefixed colon or other
     * delimiters.
     *
     * @param names the parameter names from SQL statement
     * @return New {@link ParsedParameters} instance
     * @throws IllegalArgumentException if names list contains positional parameter
     */
    public static ParsedParameters named(List<String> names) {
        if (names.contains(ParsedSql.POSITIONAL_PARAM)) {
            throw new IllegalArgumentException("Named parameters "
                + "list must not contain positional parameter "
                + "\"" + ParsedSql.POSITIONAL_PARAM + "\"");
        }
        return new ParsedParameters(false, names);
    }

    /**
     * A static factory of positional {@link ParsedParameters} instances.
     * The count given in the argument must indicate how many positional
     * parameters are available in the statement.
     *
     * @param count the number of positional parameters in statement
     * @return New {@link ParsedParameters} instance
     */
    public static ParsedParameters positional(int count) {
        return new ParsedParameters(true, Collections.nCopies(count, "?"));
    }
}

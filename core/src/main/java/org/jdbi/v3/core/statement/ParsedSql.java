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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The SQL and parameters parsed from an SQL statement.
 */
public class ParsedSql {
    private static final String POSITIONAL_PARAM = "?";

    private final String sql;
    private final ParsedParameters parameters;

    private ParsedSql(String sql, ParsedParameters parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    /**
     * @return an SQL string suitable for use with a JDBC
     * {@link java.sql.PreparedStatement}.
     */
    public String getSql() {
        return sql;
    }

    /**
     * @return the set of parameters parsed from the input SQL string.
     */
    public ParsedParameters getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParsedSql that = (ParsedSql) o;
        return Objects.equals(sql, that.sql)
            && Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, parameters);
    }

    @Override
    public String toString() {
        return "ParsedSql{"
            + "sql='" + sql + '\''
            + ", parameters=" + parameters
            + '}';
    }

    /**
     * @return a new ParsedSql builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for ParsedSql instances.
     */
    public static class Builder {
        private Builder() {}

        private final StringBuilder sql = new StringBuilder();
        private boolean positional = false;
        private boolean named = false;
        private final List<String> parameterNames = new ArrayList<>();

        /**
         * Appends the given SQL fragment to the SQL string.
         *
         * @param sqlFragment the SQL fragment
         * @return this
         */
        public Builder append(String sqlFragment) {
            sql.append(sqlFragment);
            return this;
        }

        /**
         * Records a positional parameters, and appends a <code>?</code> to the
         * SQL string.
         *
         * @return this
         */
        public Builder appendPositionalParameter() {
            positional = true;
            parameterNames.add(POSITIONAL_PARAM);
            return append("?");
        }

        /**
         * Records a named parameter with the given name, and appends a
         * <code>?</code> to the SQL string.
         *
         * @param name the parameter name.
         * @return this
         */
        public Builder appendNamedParameter(String name) {
            named = true;
            parameterNames.add(name);
            return append("?");
        }

        /**
         * @return the finalized {@link ParsedSql} object.
         */
        public ParsedSql build() {
            if (positional && named) {
                throw new UnableToExecuteStatementException(
                        "Cannot mix named and positional parameters in a SQL statement: " + parameterNames);
            }

            ParsedParameters parameters = new ParsedParameters(positional, parameterNames);

            return new ParsedSql(sql.toString(), parameters);
        }
    }
}

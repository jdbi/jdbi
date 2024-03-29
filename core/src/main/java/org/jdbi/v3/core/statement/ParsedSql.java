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
public final class ParsedSql {
    static final String POSITIONAL_PARAM = "?";

    private final String sql;
    private final ParsedParameters parameters;

    private ParsedSql(String sql, ParsedParameters parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    /**
     * Returns a SQL string suitable for use with a JDBC {@link java.sql.PreparedStatement}.
     *
     * @return a SQL string suitable for use with a JDBC {@link java.sql.PreparedStatement}.
     */
    public String getSql() {
        return sql;
    }

    /**
     * The set of parameters parsed from the input SQL string.
     *
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
     * A static factory of {@link ParsedSql} instances. The statement
     * may contain only positional parameters
     * (the {@value #POSITIONAL_PARAM} character). If your SQL
     * code contains named parameters (for example variables preceded
     * by a colon) then you have to replace them with positional
     * parameters and specify the mapping in the
     * {@link ParsedParameters}. You cannot mix named and positional
     * parameters in one SQL statement.
     *
     * @param sql the SQL code containing only positional parameters
     * @param parameters the ordered list of named parameters, or positional parameters
     * @return New {@link ParsedSql} instance
     * @see ParsedParameters#positional(int)
     * @see ParsedParameters#named(List)
     */
    public static ParsedSql of(String sql, ParsedParameters parameters) {
        return new ParsedSql(sql, parameters);
    }

    /**
     * Creates a new ParsedSql builder.
     *
     * @return a builder instance.
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
            return append(POSITIONAL_PARAM);
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
            return append(POSITIONAL_PARAM);
        }

        /**
         * Returns the finalized {@link ParsedSql} object.
         *
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

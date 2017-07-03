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
package org.jdbi.v3.core.rewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jdbi.v3.core.statement.UnableToExecuteStatementException;

public class ParsedSql {
    private static final String POSITIONAL_PARAM = "?";

    private final String sql;
    private final ParsedParameters parameters;

    private ParsedSql(String sql, ParsedParameters parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    public String getSql() {
        return sql;
    }

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
        return Objects.equals(sql, that.sql) &&
                Objects.equals(parameters, that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sql, parameters);
    }

    @Override
    public String toString() {
        return "ParsedSql{" +
                "sql='" + sql + '\'' +
                ", parameters=" + parameters +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Builder() {
        }

        private final StringBuilder sql = new StringBuilder();
        private boolean positional = false;
        private boolean named = false;
        private final List<String> parameterNames = new ArrayList<>();

        public Builder append(String sqlFragment) {
            sql.append(sqlFragment);
            return this;
        }

        public Builder appendPositionalParameter() {
            positional = true;
            parameterNames.add(POSITIONAL_PARAM);
            return append("?");
        }

        public Builder appendNamedParameter(String name) {
            named = true;
            parameterNames.add(name);
            return append("?");
        }

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

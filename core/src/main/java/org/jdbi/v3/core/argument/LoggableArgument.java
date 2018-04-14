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
package org.jdbi.v3.core.argument;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import org.jdbi.v3.core.statement.StatementContext;

/**
 * A wrapper for {@link Argument}s that enables retrieving (e.g. for logging, see {@link org.jdbi.v3.core.statement.SqlLogger}) of the represented value.
 */
public class LoggableArgument implements Argument {
    private final Object value;
    private final Argument argument;

    public LoggableArgument(Object value, Argument argument) {
        this.value = value;
        this.argument = argument;
    }

    public Object getValue() {
        return value;
    }

    public Argument getArgument() {
        return argument;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext context) throws SQLException {
        argument.apply(position, statement, context);
    }

    @Override
    public String toString() {
        return Objects.toString(value);
    }
}

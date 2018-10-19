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
package org.jdbi.v3.core.argument.internal;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.statement.StatementContext;

// TODO remove all uses of this
/**
 * @deprecated this strategy is such a bad idea...
 */
@Deprecated
public class LoggableToStringOrNPEArgument<T> implements Argument {
    private final T value;
    private final Function<T, String> toString;

    public LoggableToStringOrNPEArgument(T value) {
        this.value = value;
        this.toString = Object::toString;
    }

    public LoggableToStringOrNPEArgument(T value, Function<T, String> toString) {
        this.value = value;
        this.toString = toString;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        statement.setString(position, toString.apply(value));
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

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
package org.jdbi.v3.core.argument.internal.strategies;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

import org.jdbi.v3.core.statement.StatementContext;

// TODO remove all uses of this
/**
 * @deprecated this strategy is such a bad idea...
 */
@Deprecated
public class LoggableToStringOrNPEArgument<T> extends AbstractLoggableArgument<T> {
    private final Function<T, String> toString;

    public LoggableToStringOrNPEArgument(T value) {
        super(value);
        this.toString = Object::toString;
    }

    public LoggableToStringOrNPEArgument(T value, Function<T, String> toString) {
        super(value);
        this.toString = toString;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        statement.setString(position, toString.apply(value));
    }
}

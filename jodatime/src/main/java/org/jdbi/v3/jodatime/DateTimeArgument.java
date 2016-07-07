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
package org.jdbi.v3.jodatime;

import java.lang.reflect.Type;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.util.GenericTypes;
import org.joda.time.DateTime;

public class DateTimeArgument implements Argument {

    private final DateTime value;

    public DateTimeArgument(DateTime value) {
        this.value = value;
    }

    @Override
    public void apply(int position, PreparedStatement statement, StatementContext ctx) throws SQLException {
        if (value == null) {
            statement.setNull(position, java.sql.Types.TIMESTAMP);
        } else {
            statement.setTimestamp(position, new Timestamp(value.getMillis()));
        }
    }

    public static class Factory implements ArgumentFactory {
        @Override
        public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
            return (GenericTypes.getErasedType(type).equals(DateTime.class))
                    ? Optional.of(new DateTimeArgument((DateTime) value))
                    : Optional.empty();
        }
    }
}

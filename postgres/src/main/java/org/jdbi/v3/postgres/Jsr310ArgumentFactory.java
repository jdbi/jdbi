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
package org.jdbi.v3.postgres;

import java.lang.reflect.Type;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.jdbi.v3.core.StatementContext;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.argument.ObjectArgument;

public class Jsr310ArgumentFactory implements ArgumentFactory {

    @Override
    public Optional<Argument> build(Type type, Object value, StatementContext ctx) {
        if (type == LocalDate.class) {
            return Optional.of(new ObjectArgument(value, Types.DATE));
        }
        if (type == LocalTime.class) {
            return Optional.of(new ObjectArgument(value, Types.TIME));
        }
        if (type == LocalDateTime.class) {
            return Optional.of(new ObjectArgument(value, Types.TIMESTAMP));
        }
        if (type == OffsetDateTime.class) {
            return Optional.of(new ObjectArgument(value, Types.TIMESTAMP_WITH_TIMEZONE));
        }
        return Optional.empty();
    }

}

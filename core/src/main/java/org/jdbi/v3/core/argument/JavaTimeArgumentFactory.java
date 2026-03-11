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

import java.lang.reflect.Type;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.jdbi.v3.core.config.ConfigRegistry;

class JavaTimeArgumentFactory extends SetObjectArgumentFactory {
    private static final Map<Class<?>, Function<Object, Argument>> TYPES = Map.of(
        ZonedDateTime.class, value -> value == null
            ? new NullArgument(Types.TIMESTAMP_WITH_TIMEZONE)
            : ObjectArgument.of(((ZonedDateTime) value).toOffsetDateTime(), Types.TIMESTAMP_WITH_TIMEZONE)
    );

    JavaTimeArgumentFactory() {
        super(Map.of(Instant.class, Types.TIMESTAMP,
            LocalDate.class, Types.DATE,
            LocalTime.class, Types.TIME,
            LocalDateTime.class, Types.TIMESTAMP,
            OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE
        ));
    }

    @Override
    public Optional<Function<Object, Argument>> prepare(Type type, ConfigRegistry config) {
        var res = Optional.of(type)
            .filter(Class.class::isInstance)
            .map(Class.class::cast)
            .map(TYPES::get);

        if (res.isEmpty()) {
            res = super.prepare(type, config);
        }

        return res;
    }

}

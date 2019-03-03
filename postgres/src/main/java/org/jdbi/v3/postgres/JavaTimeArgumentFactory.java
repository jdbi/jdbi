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

import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.argument.SetObjectArgumentFactory;

/**
 * Maps {@link LocalDate}, {@link LocalTime}, {@link LocalDateTime}, {@link OffsetDateTime}.
 * Note that no {@link java.time.Instant} override is needed.
 */
public class JavaTimeArgumentFactory extends SetObjectArgumentFactory {
    private static Map<Class<?>, Integer> types() {
        final Map<Class<?>, Integer> types = new HashMap<>();
        types.put(LocalDate.class, Types.DATE);
        types.put(LocalTime.class, Types.TIME);
        types.put(LocalDateTime.class, Types.TIMESTAMP);
        types.put(OffsetDateTime.class, Types.TIMESTAMP_WITH_TIMEZONE);
        return types;
    }

    public JavaTimeArgumentFactory() {
        super(types());
    }
}

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

import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

class JavaTimeArgumentFactory extends DelegatingArgumentFactory {
    JavaTimeArgumentFactory() {
        register(Instant.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v)));
        register(LocalDate.class, Types.DATE, (p, i, v) -> p.setDate(i, java.sql.Date.valueOf(v)));
        register(LocalTime.class, Types.TIME, (p, i, v) -> p.setTime(i, Time.valueOf(v)));
        register(LocalDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.valueOf(v)));
        register(OffsetDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
        register(ZonedDateTime.class, Types.TIMESTAMP, (p, i, v) -> p.setTimestamp(i, Timestamp.from(v.toInstant())));
    }
}

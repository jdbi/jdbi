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
package org.jdbi.postgres;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.mapper.ColumnMapper;
import org.jdbi.core.mapper.ColumnMapperFactory;
import org.postgresql.util.PGInterval;

/**
 * A column mapper which maps Postgres's {@link PGInterval} type to Java's {@link Duration}.
 *
 * <p>
 * Not all {@link PGInterval}s are representable as {@link Duration}s. E.g., one with months, which is an
 * <em>estimated</em> {@link java.time.temporal.ChronoUnit}, or one whose seconds is larger than a {@code long}. An
 * {@link IllegalArgumentException} will be thrown in either case.
 */
public class DurationColumnMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (type != Duration.class) {
            return Optional.empty();
        }
        return Optional.of((r, i, c) -> {
            final Object obj = r.getObject(i);
            if (obj == null) {
                return null;
            }
            if (!(obj instanceof PGInterval interval)) {
                throw new IllegalArgumentException(String.format("got non-pginterval %s", obj));
            }
            if (interval.getYears() != 0 || interval.getMonths() != 0) {
                throw new IllegalArgumentException(
                        String.format("pginterval \"%s\" not representable as duration", interval.getValue()));
            }
            final double seconds = interval.getSeconds();
            if (seconds > Long.MAX_VALUE || seconds < Long.MIN_VALUE) {
                throw new IllegalArgumentException(
                        String.format("pginterval \"%s\" has seconds too extreme to represent as duration",
                                interval.getValue()));
            }
            final long secondsLong = (long) seconds;
            final long nanos = BigDecimal.valueOf(seconds)
                    .subtract(BigDecimal.valueOf(secondsLong))
                    .movePointRight(9)
                    .longValue();
            return Duration.ofDays(interval.getDays())
                    .plusHours(interval.getHours())
                    .plusMinutes(interval.getMinutes())
                    .plusSeconds(secondsLong)
                    .plusNanos(nanos);
        });
    }
}

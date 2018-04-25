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

import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMapperFactory;
import org.postgresql.util.PGInterval;

import java.lang.reflect.Type;
import java.time.Period;
import java.util.Optional;

/**
 * A column mapper which maps Postgres's {@link PGInterval} type to Java's {@link Period}.
 *
 * <p>
 * Not all {@link PGInterval}s are representable as {@link Period}s. E.g., one with minutes. An
 * {@link IllegalArgumentException} will be thrown in this case.
 */
public class PeriodColumnMapperFactory implements ColumnMapperFactory {
    @Override
    public Optional<ColumnMapper<?>> build(Type type, ConfigRegistry config) {
        if (type != Period.class) {
            return Optional.empty();
        }
        return Optional.of((r, i, c) -> {
            final Object obj = r.getObject(i);
            if (obj == null) {
                return null;
            }
            if (!(obj instanceof PGInterval)) {
                throw new IllegalArgumentException(String.format("got non-pginterval %s", obj));
            }
            final PGInterval interval = (PGInterval) obj;
            if (interval.getHours() != 0 || interval.getMinutes() != 0 || interval.getSeconds() != 0) {
                throw new IllegalArgumentException(
                        String.format("pginterval \"%s\" is too granular to be represented as period",
                                interval.getValue()));
            }
            return Period.of(interval.getYears(), interval.getMonths(), interval.getDays());
        });
    }
}

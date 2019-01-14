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
import java.time.Duration;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ObjectArgument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.postgresql.util.PGInterval;

/**
 * Postgres version of argument factory for {@link Duration}.
 *
 * <p>
 * For simplicity, this implementation makes the duration positive before proceeding. However, this can cause an
 * {@link ArithmeticException} to be thrown. E.g., this can occur if your duration is -2^63 seconds.
 *
 * <p>
 * Not all {@link Duration}s can be represented as intervals in Postgres.
 * One incompatibility results from {@link Duration}s that are too large. This is due to (1) {@link Duration}s using
 * a {@code long} internally, and {@link PGInterval}s using {@code int}s; and (2) the conversion of days to months or
 * years being ambiguous.
 * Another results from {@link Duration}s being too precise; they have nanosecond precision, whereas Postgres has only
 * microsecond.
 * An {@link IllegalArgumentException} will be thrown in these cases.
 * The handling of the second is subject to revision in the future; for example, it would be reasonable to have a
 * configurable truncation option.
 */
public class DurationArgumentFactory extends AbstractArgumentFactory<Duration> {

    public DurationArgumentFactory() {
        super(Types.OTHER);
    }

    @Override
    public Argument build(Duration duration, ConfigRegistry config) {
        Duration d = duration;
        final boolean isNegative = d.isNegative();
        if (isNegative) {
            d = d.negated();
        }
        final long days = d.toDays();
        if (days > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    String.format("duration %s too large to be represented unambiguously as postgres interval",
                            d));
        }
        d = d.minusDays(days);
        final int hours = (int) d.toHours();
        d = d.minusHours(hours);
        final int minutes = (int) d.toMinutes();
        d = d.minusMinutes(minutes);
        if (d.getNano() % 1000 != 0) {
            throw new IllegalArgumentException(
                    String.format("duration %s too precise to represented as postgres interval", d));
        }
        double seconds = d.getSeconds() + d.getNano() / 1e9;
        final PGInterval interval = new PGInterval(0, 0, (int) days, hours, minutes, seconds);
        if (isNegative) {
            interval.scale(-1);
        }
        return new ObjectArgument(interval, Types.OTHER);
    }
}

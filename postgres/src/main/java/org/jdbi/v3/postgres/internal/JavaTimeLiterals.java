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
package org.jdbi.v3.postgres.internal;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.chrono.IsoEra;
import java.time.temporal.ChronoField;
import java.util.Locale;

import org.jdbi.v3.core.internal.UtilityClassException;

/**
 * Formats {@code java.time} values as Postgres text literals for SQL array binding.
 * <p>
 * The driver sends array elements it has no native encoder for as text, so the element
 * conversion must produce text the server parses for the declared element type. The ISO-8601
 * form produced by {@code toString()} covers years 1 through 9999 only; these methods also
 * cover BC dates and years with more than four digits, which Postgres expects as
 * {@code 0501-01-01 BC} and {@code 10000-01-01} rather than {@code -0500-01-01} and
 * {@code +10000-01-01}. The era suffix follows the zone offset, matching Postgres output.
 * <p>
 * Values at or beyond the range endpoints become {@code infinity} and {@code -infinity},
 * with the same cutoffs the driver's own scalar rendering uses ({@code TimestampUtils}):
 * the maximum {@code java.time} value maps to {@code infinity}, and anything before
 * 4713-01-01 BC, the lowest date Postgres accepts, maps to {@code -infinity}.
 * <p>
 * This class works around <a href="https://github.com/pgjdbc/pgjdbc/issues/4399">pgjdbc#4399</a>:
 * the driver renders scalar temporal values through {@code TimestampUtils} but renders array
 * elements with {@code Object.toString()}. Once the driver routes array elements through
 * {@code TimestampUtils} as well, the identity conversion is sufficient and this class can be
 * deleted.
 */
public final class JavaTimeLiterals {

    private static final LocalDate MIN_DATE = LocalDate.of(4713, 1, 1).with(ChronoField.ERA, IsoEra.BCE.getValue());
    private static final LocalDateTime MIN_TIMESTAMP = MIN_DATE.atStartOfDay();
    private static final OffsetDateTime MIN_OFFSET_TIMESTAMP = MIN_TIMESTAMP.atOffset(ZoneOffset.UTC);
    // the driver leaves 500ms of headroom for its own rounding; match it so scalar and array binds agree
    private static final LocalDateTime MAX_TIMESTAMP = LocalDateTime.MAX.minus(Duration.ofMillis(500));
    private static final OffsetDateTime MAX_OFFSET_TIMESTAMP = OffsetDateTime.MAX.minus(Duration.ofMillis(500));

    private JavaTimeLiterals() {
        throw new UtilityClassException();
    }

    public static String dateLiteral(LocalDate date) {
        if (date == null) {
            return null;
        }
        if (LocalDate.MAX.equals(date)) {
            return "infinity";
        }
        if (date.isBefore(MIN_DATE)) {
            return "-infinity";
        }
        return withEra(isoDate(date), date.getYear());
    }

    public static String timestampLiteral(LocalDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp.isAfter(MAX_TIMESTAMP)) {
            return "infinity";
        }
        if (timestamp.isBefore(MIN_TIMESTAMP)) {
            return "-infinity";
        }
        return withEra(isoDate(timestamp.toLocalDate()) + " " + timestamp.toLocalTime(), timestamp.getYear());
    }

    public static String timestampLiteral(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return null;
        }
        if (timestamp.isAfter(MAX_OFFSET_TIMESTAMP)) {
            return "infinity";
        }
        if (timestamp.isBefore(MIN_OFFSET_TIMESTAMP)) {
            return "-infinity";
        }
        return withEra(isoDate(timestamp.toLocalDate()) + " " + timestamp.toLocalTime() + offsetOf(timestamp),
                timestamp.getYear());
    }

    private static String isoDate(LocalDate date) {
        int year = date.getYear();
        // Locale.ROOT: %d substitutes locale digits under e.g. Arabic locales, which the server cannot parse
        return String.format(Locale.ROOT, "%04d-%02d-%02d",
                year <= 0 ? 1 - year : year, date.getMonthValue(), date.getDayOfMonth());
    }

    private static String withEra(String literal, int year) {
        return year <= 0 ? literal + " BC" : literal;
    }

    private static String offsetOf(OffsetDateTime timestamp) {
        ZoneOffset offset = timestamp.getOffset();
        return ZoneOffset.UTC.equals(offset) ? "+00:00" : offset.getId();
    }
}

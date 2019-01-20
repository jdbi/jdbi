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

import java.util.Map;
import java.util.UUID;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.spi.JdbiPlugin;

/**
 * Postgres plugin. Adds support for binding and mapping the following data types:
 *
 * <ul>
 * <li>{@link java.net.InetAddress} (including {@link java.net.Inet4Address} and {@link java.net.Inet6Address})</li>
 * <li>{@link java.time.LocalDate}</li>
 * <li>{@link java.time.LocalTime}</li>
 * <li>{@link java.time.LocalDateTime}</li>
 * <li>{@link java.time.OffsetDateTime}</li>
 * <li>{@link java.time.Duration} (see notes below)</li>
 * <li>{@link java.time.Period} (see notes below)</li>
 * <li>{@link java.util.Map Map&lt;String, String&gt;} (for {@code HSTORE} columns)</li>
 * <li>{@link java.util.UUID}</li>
 * </ul>
 *
 * <p>
 * The following qualified types have {@link org.jdbi.v3.meta.Beta} support for binding and mapping:
 *
 * <ul>
 * <li>{@link MacAddr @MacAddr java.lang.String} (for MACADDR columns)</li>
 * <li>{@link HStore @HStore Map&lt;String, String&gt;} (for HSTORE columns)</li>
 * </ul>
 *
 * <p>
 * Also sets up SQL array support for the following types:
 *
 * <ul>
 * <li>{@code double}</li>
 * <li>{@code float}</li>
 * <li>{@code int}</li>
 * <li>{@code long}</li>
 * <li>{@link java.lang.Double}</li>
 * <li>{@link java.lang.Float}</li>
 * <li>{@link java.lang.Integer}</li>
 * <li>{@link java.lang.Long}</li>
 * <li>{@link java.lang.String}</li>
 * <li>{@link java.util.UUID}</li>
 * </ul>
 *
 * <p>
 * A note about the mapping between the Postgres {@code interval} type and the Java {@link java.time.Period} and
 * {@link java.time.Duration} types:
 * The Java library authors are much more strict about the temporal amounts representable by Periods and Durations than
 * Postgres is with its {@code interval} type.  The argument factories and column mapper factories implemented in this
 * package respect this spirit of unambiguous strictness.  Consequently:
 * <ul>
 *     <li>All {@link java.time.Period}s can be mapped to {@code interval}s.</li>
 *     <li>Not all {@link java.time.Duration}s can be mapped to {@code interval}s.</li>
 *     <li>Not all {@code interval}s can be mapped to {@link java.time.Period}s.</li>
 *     <li>Not all {@code interval}s can be mapped to {@link java.time.Duration}s.</li>
 * </ul>
 * For more specific detail, see the caveats in the documentation for {@link DurationArgumentFactory},
 * {@link PeriodColumnMapperFactory}, and {@link DurationColumnMapperFactory}.
 *
 * <p>
 * In addition, some potentially unexpected implicit conversions can occur by virtue of the Postgres <em>server</em>
 * logic. For example, at the time of writing, storing a Period of -3 years, 2 months, and -1 days results in an
 * interval (and consequently, a column-mapped Period) of <em>-2 years, -10 months</em>, and -1 days.
 */
public class PostgresPlugin implements JdbiPlugin {
    @Override
    public void customizeJdbi(Jdbi jdbi) {
        jdbi.registerArgument(new TypedEnumArgumentFactory());
        jdbi.registerArgument(new JavaTimeArgumentFactory());
        jdbi.registerArgument(new DurationArgumentFactory());
        jdbi.registerArgument(new PeriodArgumentFactory());
        jdbi.registerArgument(new InetArgumentFactory());
        jdbi.registerArgument(new HStoreArgumentFactory());
        jdbi.registerArgument(new MacAddrArgumentFactory());
        jdbi.registerArgument(new UUIDArgumentFactory());
        jdbi.registerArgument(new PGObjectArgumentFactory());
        jdbi.registerArgument(new PGObjectArrayArgumentFactory());

        jdbi.registerArrayType(int.class, "integer");
        jdbi.registerArrayType(Integer.class, "integer");
        jdbi.registerArrayType(long.class, "bigint");
        jdbi.registerArrayType(Long.class, "bigint");
        jdbi.registerArrayType(String.class, "varchar");
        jdbi.registerArrayType(UUID.class, "uuid");
        jdbi.registerArrayType(float.class, "real");
        jdbi.registerArrayType(Float.class, "real");
        jdbi.registerArrayType(double.class, "double precision");
        jdbi.registerArrayType(Double.class, "double precision");

        jdbi.registerColumnMapper(new JavaTimeMapperFactory());

        jdbi.registerColumnMapper(new HStoreColumnMapper());
        jdbi.registerColumnMapper(new MacAddrColumnMapper());
        jdbi.registerColumnMapper(new DurationColumnMapperFactory());
        jdbi.registerColumnMapper(new PeriodColumnMapperFactory());
        jdbi.registerColumnMapper(new PGObjectColumnMapper());

        // legacy unqualified HSTORE
        jdbi.registerArgument(new HStoreArgumentFactory()::build);
        jdbi.registerColumnMapper(new GenericType<Map<String, String>>() {}, new HStoreColumnMapper());

        // optional integration
        if (JdbiClassUtils.isPresent("org.jdbi.v3.json.JsonConfig")) {
            jdbi.registerArgument(new JsonArgumentFactory());
        }
    }
}

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
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.spi.JdbiPlugin;

/**
 * Postgres plugin.
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
    public void customizeJdbi(Jdbi db) {
        db.registerArgument(new TypedEnumArgumentFactory());
        db.registerArgument(new JavaTimeArgumentFactory());
        db.registerArgument(new DurationArgumentFactory());
        db.registerArgument(new PeriodArgumentFactory());
        db.registerArgument(new InetArgumentFactory());
        db.registerArgument(new HStoreArgumentFactory());
        db.registerArgument(new UUIDArgumentFactory());

        db.registerArrayType(int.class, "integer");
        db.registerArrayType(Integer.class, "integer");
        db.registerArrayType(long.class, "bigint");
        db.registerArrayType(Long.class, "bigint");
        db.registerArrayType(String.class, "varchar");
        db.registerArrayType(UUID.class, "uuid");
        db.registerArrayType(float.class, "real");
        db.registerArrayType(Float.class, "real");
        db.registerArrayType(double.class, "double precision");
        db.registerArrayType(Double.class, "double precision");

        db.registerColumnMapper(new JavaTimeMapperFactory());
        db.registerColumnMapper(new HStoreColumnMapper());
        QualifiedType hstoreMapType = QualifiedType.of(
            new GenericType<Map<String, String>>() {},
            PostgresQualifiers.HSTORE);
        db.registerColumnMapper(hstoreMapType, new HStoreColumnMapper());
        db.registerColumnMapper(new DurationColumnMapperFactory());
        db.registerColumnMapper(new PeriodColumnMapperFactory());
    }
}

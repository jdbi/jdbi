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

import java.sql.Connection;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.core.internal.exceptions.Unchecked;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.postgres.internal.BitStringEnumSetArgumentFactory;
import org.jdbi.v3.postgres.internal.BitStringEnumSetMapperFactory;
import org.jdbi.v3.postgres.internal.ByteaArrayType;
import org.postgresql.PGConnection;
import org.postgresql.geometric.PGbox;
import org.postgresql.geometric.PGcircle;
import org.postgresql.geometric.PGline;
import org.postgresql.geometric.PGlseg;
import org.postgresql.geometric.PGpath;
import org.postgresql.geometric.PGpoint;
import org.postgresql.geometric.PGpolygon;
import org.postgresql.util.PGInterval;
import org.postgresql.util.PGmoney;

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
 * <li>{@link java.io.InputStream} and {@link java.io.Reader} from {@code oid} large object columns</li>
 * <li>@MacAddr {@link java.lang.String} (for {@code MACADDR} columns)</li>
 * <li>@HStore {@link Map} (for {@code HSTORE} columns)</li>
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
public class PostgresPlugin extends JdbiPlugin.Singleton {

    private final boolean installLegacy;

    /**
     * Do not install the legacy (unqualified) bindings for {@link HStoreArgumentFactory} and {@link HStoreColumnMapper}. When using the plugin returned by this
     * factory method, any lookup for HStore specific arguments and column mappers must be qualified with the {@link HStore} annotation.
     */
    public static PostgresPlugin noUnqualifiedHstoreBindings() {
        return new PostgresPlugin(false);
    }

    public PostgresPlugin() {
        this(true);
    }

    protected PostgresPlugin(boolean installLegacy) {
        this.installLegacy = installLegacy;
    }

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
        jdbi.registerArgument(new PGobjectArgumentFactory());
        jdbi.registerArgument(new BitStringEnumSetArgumentFactory());
        jdbi.registerArgument(new BlobInputStreamArgumentFactory());
        jdbi.registerArgument(new ClobReaderArgumentFactory());

        // built-in PGobject types
        jdbi.registerArrayType(PGbox.class, "box");
        jdbi.registerArrayType(PGcircle.class, "circle");
        jdbi.registerArrayType(PGInterval.class, "interval");
        jdbi.registerArrayType(PGline.class, "line");
        jdbi.registerArrayType(PGlseg.class, "lseg");
        jdbi.registerArrayType(PGmoney.class, "money");
        jdbi.registerArrayType(PGpath.class, "path");
        jdbi.registerArrayType(PGpoint.class, "point");
        jdbi.registerArrayType(PGpolygon.class, "polygon");
        jdbi.registerArrayType(new ByteaArrayType());

        jdbi.registerColumnMapper(new JavaTimeMapperFactory());
        jdbi.registerColumnMapper(new HStoreColumnMapper());
        jdbi.registerColumnMapper(new MacAddrColumnMapper());
        jdbi.registerColumnMapper(new DurationColumnMapperFactory());
        jdbi.registerColumnMapper(new PeriodColumnMapperFactory());
        jdbi.registerColumnMapper(new PGobjectColumnMapperFactory());
        jdbi.registerColumnMapper(new BitStringEnumSetMapperFactory());
        jdbi.registerColumnMapper(new BlobInputStreamColumnMapperFactory());
        jdbi.registerColumnMapper(new ClobReaderColumnMapperFactory());

        if (installLegacy) {
            // legacy unqualified HSTORE
            // Do *NOT* replace with `new HStoreArgumentFactory()`, the AI/Intellij whatever hint is wrong.
            jdbi.registerArgument((ArgumentFactory) new HStoreArgumentFactory()::build);
            jdbi.registerColumnMapper(new GenericType<>() {}, new HStoreColumnMapper());
        }

        // optional integration
        if (JdbiClassUtils.isPresent("org.jdbi.v3.json.JsonConfig")) {
            jdbi.registerArgument(new JsonArgumentFactory());
        }
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public Handle customizeHandle(Handle handle) {
        Connection conn = handle.getConnection();
        PGConnection pgConnection = Unchecked.supplier(() -> conn.unwrap(PGConnection.class)).get();
        return handle.configure(PostgresTypes.class, pt -> {
            pt.addTypesToConnection(pgConnection);
            pt.setLobApi(new PgLobApiImpl(conn));
        });
    }
}

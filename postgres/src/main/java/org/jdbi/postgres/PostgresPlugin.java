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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.pgvector.PGbit;
import com.pgvector.PGvector;
import org.jdbi.core.Jdbi;
import org.jdbi.core.argument.ArgumentFactory;
import org.jdbi.core.config.ConfigRegistry;
import org.jdbi.core.generic.GenericType;
import org.jdbi.core.internal.JdbiClassUtils;
import org.jdbi.core.internal.UtilityClassException;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.postgres.internal.BitStringEnumSetArgumentFactory;
import org.jdbi.postgres.internal.BitStringEnumSetMapperFactory;
import org.jdbi.postgres.internal.ByteaArrayType;
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

    private static final boolean PGVECTOR_AVAILABLE = JdbiClassUtils.isPresent("com.pgvector.PGvector");

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
    public void configure(Jdbi.Builder builder) {
        builder.registerArgument(new TypedEnumArgumentFactory());
        builder.registerArgument(new JavaTimeArgumentFactory());
        builder.registerArgument(new DurationArgumentFactory());
        builder.registerArgument(new PeriodArgumentFactory());
        builder.registerArgument(new InetArgumentFactory());
        builder.registerArgument(new HStoreArgumentFactory());
        builder.registerArgument(new MacAddrArgumentFactory());
        builder.registerArgument(new UUIDArgumentFactory());
        builder.registerArgument(new PGobjectArgumentFactory());
        builder.registerArgument(new BitStringEnumSetArgumentFactory());
        builder.registerArgument(new BlobInputStreamArgumentFactory());
        builder.registerArgument(new ClobReaderArgumentFactory());

        // built-in PGobject types
        builder.registerArrayType(PGbox.class, "box");
        builder.registerArrayType(PGcircle.class, "circle");
        builder.registerArrayType(PGInterval.class, "interval");
        builder.registerArrayType(PGline.class, "line");
        builder.registerArrayType(PGlseg.class, "lseg");
        builder.registerArrayType(PGmoney.class, "money");
        builder.registerArrayType(PGpath.class, "path");
        builder.registerArrayType(PGpoint.class, "point");
        builder.registerArrayType(PGpolygon.class, "polygon");
        builder.registerArrayType(new ByteaArrayType());
        builder.registerArrayType(new PostgresCustomTypeArrayFactory());

        builder.registerColumnMapper(new JavaTimeMapperFactory());
        builder.registerColumnMapper(new HStoreColumnMapper());
        builder.registerColumnMapper(new MacAddrColumnMapper());
        builder.registerColumnMapper(new DurationColumnMapperFactory());
        builder.registerColumnMapper(new PeriodColumnMapperFactory());
        builder.registerColumnMapper(new PGobjectColumnMapperFactory());
        builder.registerColumnMapper(new BitStringEnumSetMapperFactory());
        builder.registerColumnMapper(new BlobInputStreamColumnMapperFactory());
        builder.registerColumnMapper(new ClobReaderColumnMapperFactory());

        if (installLegacy) {
            // legacy unqualified HSTORE
            // Do *NOT* replace with `new HStoreArgumentFactory()`, the AI/Intellij whatever hint is wrong.
            builder.registerArgument((ArgumentFactory) new HStoreArgumentFactory()::build);
            builder.registerColumnMapper(new GenericType<>() {}, new HStoreColumnMapper());
        }

        // optional integration
        if (JdbiClassUtils.isPresent("org.jdbi.json.JsonConfig")) {
            builder.registerArgument(new JsonArgumentFactory());
        }
    }

    @Override
    public Connection customizeConnection(Connection conn) throws SQLException {
        if (PGVECTOR_AVAILABLE) {
            VectorEnabler.enable(conn);
        }
        return conn;
    }

    @Override
    public void customizeHandleConfig(Connection connection, ConfigRegistry config) throws SQLException {
        // Register the configured custom types on this physical connection. This is a driver-level side effect
        // on the connection using the Jdbi-level type registrations; it stores no per-handle configuration (the
        // handle's config child stays unforked), and the Large Object API is now resolved per statement from the
        // connection (see BlobInputStream{Argument,ColumnMapper}Factory), so nothing connection-bound is cached here.
        PGConnection pgConnection = connection.unwrap(PGConnection.class);
        config.get(PostgresTypes.class).addTypesToConnection(pgConnection);
    }

    static final class VectorEnabler {
        private VectorEnabler() {
            throw new UtilityClassException();
        }
        static void enable(Connection conn) throws SQLException {
            PGvector.registerTypes(conn);
            PGbit.registerType(conn);
        }
    }
}

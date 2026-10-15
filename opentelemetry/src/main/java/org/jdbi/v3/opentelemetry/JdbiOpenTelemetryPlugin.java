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
package org.jdbi.v3.opentelemetry;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionMethod;
import org.jdbi.v3.core.spi.JdbiPlugin;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.core.statement.StatementContextListener;

/**
 * Enable OpenTelemetry support.
 * Emits a {@code CLIENT} span for every statement executed by Jdbi, with the attributes of the
 * <a href="https://opentelemetry.io/docs/specs/semconv/db/database-spans/">database client span semantic conventions</a>.
 * <p>
 * The span records {@code db.system.name}, {@code db.query.text}, and, for queries, {@code db.response.returned_rows}.
 * A statement from a SQL Object method sets {@code db.query.summary} to {@code Type.method}, which is also the span name.
 * Other statements use {@code db.system.name} as the span name.
 * <p>
 * When the statement execution fails, the span status is {@code ERROR}. If a {@link SQLException} caused the failure,
 * {@code db.response.status_code} is its vendor error code for MySQL, MariaDB, Microsoft SQL Server, and Oracle
 * (as {@code ORA-nnnnn}), and its SQLSTATE for other databases. {@code error.type} is the status code if there is one,
 * else the class name of the {@code SQLException}, else the class name of the exception that failed the statement.
 * A {@link org.jdbi.v3.core.statement.SqlExceptionHandler} that replaces the exception does not change these values.
 * <p>
 * Bound values are recorded only when {@link SqlStatements#setIncludeBindingsInTelemetry(boolean)} is set,
 * as one {@code db.query.parameter.<key>} attribute per positional or named argument, and never for batches.
 */
public class JdbiOpenTelemetryPlugin extends JdbiPlugin.Singleton {
    /**
     * @deprecated Spans no longer have this attribute. The query text is in {@code db.query.text}.
     */
    @Deprecated(since = "3.56.0", forRemoval = true)
    public static final AttributeKey<String> SQL = AttributeKey.stringKey("sql");
    /**
     * @deprecated Spans no longer have this attribute. Bound values are in {@code db.query.parameter.<key>}.
     */
    @Deprecated(since = "3.56.0", forRemoval = true)
    public static final AttributeKey<String> BINDING = AttributeKey.stringKey("binding");
    /**
     * @deprecated Spans no longer have this attribute. The row count of a query is in {@code db.response.returned_rows}.
     */
    @Deprecated(since = "3.56.0", forRemoval = true)
    public static final AttributeKey<Long> NUM_ROWS = AttributeKey.longKey("rows");

    static final AttributeKey<String> DB_SYSTEM_NAME = AttributeKey.stringKey("db.system.name");
    static final AttributeKey<String> DB_QUERY_SUMMARY = AttributeKey.stringKey("db.query.summary");
    static final AttributeKey<String> DB_QUERY_TEXT = AttributeKey.stringKey("db.query.text");
    static final String DB_QUERY_PARAMETER_PREFIX = "db.query.parameter.";
    static final AttributeKey<Long> DB_RESPONSE_RETURNED_ROWS = AttributeKey.longKey("db.response.returned_rows");
    static final AttributeKey<String> DB_RESPONSE_STATUS_CODE = AttributeKey.stringKey("db.response.status_code");
    static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

    static final String OTHER_SQL = "other_sql";

    // Prefixes of DatabaseMetaData.getDatabaseProductName(), lowercase, mapped to the well-known db.system.name values.
    private static final List<Map.Entry<String, String>> WELL_KNOWN_SYSTEMS = List.of(
            Map.entry("postgresql", "postgresql"),
            Map.entry("mysql", "mysql"),
            Map.entry("mariadb", "mariadb"),
            Map.entry("microsoft sql server", "microsoft.sql_server"),
            Map.entry("oracle", "oracle.db"),
            Map.entry("h2", "h2database"),
            Map.entry("hsql database engine", "hsqldb"),
            Map.entry("sqlite", "sqlite"),
            Map.entry("apache derby", "derby"),
            Map.entry("db2", "ibm.db2"),
            Map.entry("informix", "ibm.informix"),
            Map.entry("firebird", "firebirdsql"),
            Map.entry("hdb", "sap.hana"),
            Map.entry("redshift", "aws.redshift"),
            Map.entry("clickhouse", "clickhouse"),
            Map.entry("trino", "trino"),
            Map.entry("teradata", "teradata"));

    // The semantic conventions for these systems use the vendor error code, not the SQLSTATE, as the status code.
    private static final Set<String> VENDOR_ERROR_CODE_SYSTEMS = Set.of("mysql", "mariadb", "microsoft.sql_server");

    private final Tracer tracer;

    /**
     * Enable OpenTelemetry support with the global OpenTelemetry instance.
     */
    public JdbiOpenTelemetryPlugin() {
        this(GlobalOpenTelemetry.get());
    }

    /**
     * Enable OpenTelemetry support with the supplied OpenTelemetry instance.
     * @param telemetry the OpenTelemetry to emit spans to
     */
    public JdbiOpenTelemetryPlugin(final OpenTelemetry telemetry) {
        tracer = telemetry.getTracer("jdbi");
    }

    @Override
    public void customizeJdbi(final Jdbi jdbi) throws SQLException {
        jdbi.getConfig(SqlStatements.class).addContextListener(new TracingListener());
    }

    static String dbSystemName(final String productName) {
        if (productName == null || productName.isBlank()) {
            return OTHER_SQL;
        }
        final String product = productName.toLowerCase(Locale.ROOT);
        return WELL_KNOWN_SYSTEMS.stream()
                .filter(e -> product.startsWith(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(product);
    }

    static String statusCode(final String dbSystemName, final SQLException e) {
        if (e.getErrorCode() != 0) {
            if (VENDOR_ERROR_CODE_SYSTEMS.contains(dbSystemName)) {
                return String.valueOf(e.getErrorCode());
            }
            if ("oracle.db".equals(dbSystemName)) {
                return String.format(Locale.ROOT, "ORA-%05d", e.getErrorCode());
            }
        }
        if (e.getSQLState() != null) {
            return e.getSQLState();
        }
        return e.getErrorCode() == 0 ? null : String.valueOf(e.getErrorCode());
    }

    static SQLException findSqlException(final Throwable failure) {
        final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable t = failure; t != null && seen.add(t); t = t.getCause()) {
            if (t instanceof SQLException sqlException) {
                return sqlException;
            }
        }
        return null;
    }

    private static boolean isStatementType(final StatementContext ctx, final Class<?> type) {
        return ctx.getJdbiStatementType() instanceof Class<?> statementType && type.isAssignableFrom(statementType);
    }

    class TracingListener implements StatementContextListener {
        // One listener exists for each Jdbi, and all connections of a Jdbi go to the same database system.
        private volatile String dbSystemName;

        @Override
        public void contextCreated(final StatementContext ctx) {
            final String system = dbSystemName(ctx.getConnection());
            final String summary = querySummary(ctx.getExtensionMethod());
            final var spanBuilder = tracer.spanBuilder(summary == null ? system : summary)
                    .setSpanKind(SpanKind.CLIENT)
                    .setAttribute(DB_SYSTEM_NAME, system);
            if (summary != null) {
                spanBuilder.setAttribute(DB_QUERY_SUMMARY, summary);
            }
            final var span = spanBuilder.startSpan();
            final var spanContext = span.getSpanContext();
            if (spanContext.isValid()) {
                ctx.setTraceId(spanContext.getTraceId());
                ctx.addCleanable(() -> {
                    try {
                        recordFailure(span, system, ctx.getFailure());
                        final var stmtConfig = ctx.getConfig(SqlStatements.class);
                        final String renderedSql = ctx.getRenderedSql();
                        if (renderedSql != null) {
                            span.setAttribute(DB_QUERY_TEXT, renderedSql.substring(0,
                                    Math.min(renderedSql.length(), stmtConfig.getJfrSqlMaxLength())));
                        }
                        final long mappedRows = ctx.getMappedRows();
                        if (isStatementType(ctx, Query.class) || mappedRows > 0) {
                            span.setAttribute(DB_RESPONSE_RETURNED_ROWS, mappedRows);
                        }
                        if (stmtConfig.getIncludeBindingsInTelemetry()
                                && !isStatementType(ctx, Batch.class)
                                && !isStatementType(ctx, PreparedBatch.class)) {
                            ctx.getBinding().describeArguments(stmtConfig.getJfrParamMaxLength())
                                    .forEach((key, value) -> span.setAttribute(DB_QUERY_PARAMETER_PREFIX + key, value));
                        }
                    } finally {
                        span.end();
                    }
                });
            }
        }

        private void recordFailure(final Span span, final String system, final Exception failure) {
            if (failure == null) {
                return;
            }
            span.setStatus(StatusCode.ERROR);
            final SQLException sqlException = findSqlException(failure);
            final String statusCode = sqlException == null ? null : statusCode(system, sqlException);
            if (statusCode != null) {
                span.setAttribute(DB_RESPONSE_STATUS_CODE, statusCode);
                span.setAttribute(ERROR_TYPE, statusCode);
            } else {
                span.setAttribute(ERROR_TYPE, (sqlException == null ? failure : sqlException).getClass().getName());
            }
        }

        private String dbSystemName(final Connection connection) {
            final String cached = dbSystemName;
            if (cached != null) {
                return cached;
            }
            if (connection == null) {
                return OTHER_SQL;
            }
            try {
                final String system = JdbiOpenTelemetryPlugin.dbSystemName(connection.getMetaData().getDatabaseProductName());
                dbSystemName = system;
                return system;
            } catch (SQLException e) {
                return OTHER_SQL;
            }
        }

        private String querySummary(final ExtensionMethod extensionMethod) {
            if (extensionMethod == null) {
                return null;
            }
            return extensionMethod.getType().getSimpleName() + "." + extensionMethod.getMethod().getName();
        }
    }
}

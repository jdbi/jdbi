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
package org.jdbi.opentelemetry;

import java.sql.SQLException;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Tracer;
import org.jdbi.core.Jdbi;
import org.jdbi.core.spi.JdbiPlugin;
import org.jdbi.core.statement.SqlStatements;
import org.jdbi.core.statement.StatementContext;
import org.jdbi.core.statement.StatementContextListener;

/**
 * Enable OpenTelemetry support.
 * Emits a trace span for every statement executed by Jdbi.
 */
public class JdbiOpenTelemetryPlugin extends JdbiPlugin.Singleton {
    public static final AttributeKey<String> SQL = AttributeKey.stringKey("sql");
    public static final AttributeKey<String> BINDING = AttributeKey.stringKey("binding");
    public static final AttributeKey<Long> NUM_ROWS = AttributeKey.longKey("rows");

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

    class TracingListener implements StatementContextListener {
        @Override
        public void contextCreated(final StatementContext ctx) {
            final var span = tracer.spanBuilder("jdbi." + ctx.describeJdbiStatementType())
                    .startSpan();
            final var spanContext = span.getSpanContext();
            if (spanContext.isValid()) {
                ctx.setTraceId(spanContext.getTraceId());
                ctx.addCleanable(() -> {
                    final var stmtConfig = ctx.getConfig(SqlStatements.class);
                    final String renderedSql = ctx.getRenderedSql();
                    if (renderedSql != null) {
                        span.setAttribute(SQL, renderedSql.substring(0,
                                Math.min(renderedSql.length(), stmtConfig.getJfrSqlMaxLength())));
                    }
                    span.setAttribute(BINDING, ctx.getBinding().describe(stmtConfig.getJfrParamMaxLength()));
                    span.setAttribute(NUM_ROWS, ctx.getMappedRows());
                    span.end();
                });
            }
        }
    }
}

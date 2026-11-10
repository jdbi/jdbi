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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.SqlStatements;
import org.jdbi.v3.testing.junit5.JdbiExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_QUERY_TEXT;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.DB_SYSTEM_NAME;
import static org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin.OTHER_SQL;

public class TestFailSafeTelemetry {
    private static final String SELECT = "select 1";

    @RegisterExtension
    JdbiExtension ext = JdbiExtension.h2();

    private final InMemorySpanExporter traces = new InMemorySpanExporter();
    private final AtomicInteger metaDataCalls = new AtomicInteger();

    @BeforeEach
    @AfterEach
    void resetGlobalOpenTelemetry() {
        GlobalOpenTelemetry.resetForTest();
    }

    @Test
    void nullMetaData() {
        final Jdbi jdbi = jdbi(connection -> null);
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(1);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, OTHER_SQL);
    }

    @Test
    void nullProductName() {
        final Jdbi jdbi = jdbi(connection -> (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] {DatabaseMetaData.class}, (proxy, method, args) -> null));
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(1);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, OTHER_SQL);
    }

    @Test
    void metaDataThrows() {
        final Jdbi jdbi = jdbi(connection -> {
            throw new IllegalStateException("no metadata");
        });
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(1);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME))
            .containsExactly(OTHER_SQL, OTHER_SQL);
    }

    @Test
    void installPluginsWithoutSdk() {
        final Jdbi jdbi = jdbi(connection -> {
            throw new IllegalStateException("no metadata");
        });
        jdbi.installPlugins();

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(0);
    }

    @Test
    void noopOpenTelemetry() {
        final Jdbi jdbi = jdbi(connection -> null);
        jdbi.installPlugin(new JdbiOpenTelemetryPlugin(OpenTelemetry.noop()));

        assertThat(selectTwice(jdbi)).containsExactly(1, 1);
        assertThat(metaDataCalls).hasValue(0);
    }

    @Test
    void samplerThrows() {
        final Sampler sampler = new Sampler() {
            @Override
            public SamplingResult shouldSample(final Context parentContext, final String traceId, final String name,
                final SpanKind spanKind, final Attributes attributes, final List<LinkData> parentLinks) {
                throw new IllegalStateException("sampler failed");
            }

            @Override
            public String getDescription() {
                return "throwing sampler";
            }
        };
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), sampler)));

        assertThat(selectTwice(ext.getJdbi())).containsExactly(1, 1);
        assertThat(traces.getExported()).isEmpty();
    }

    @Test
    void spanEndThrows() {
        final SpanProcessor processor = new SpanProcessor() {
            @Override
            public void onStart(final Context parentContext, final ReadWriteSpan span) {}

            @Override
            public boolean isStartRequired() {
                return false;
            }

            @Override
            public void onEnd(final ReadableSpan span) {
                throw new IllegalStateException("span processor failed");
            }

            @Override
            public boolean isEndRequired() {
                return true;
            }
        };
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(processor, Sampler.alwaysOn())));

        assertThat(selectTwice(ext.getJdbi())).containsExactly(1, 1);
    }

    @Test
    void attributeFailureEndsSpan() {
        ext.getJdbi().getConfig(SqlStatements.class).setJfrSqlMaxLength(-1);
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOn())));

        assertThat(selectTwice(ext.getJdbi())).containsExactly(1, 1);
        assertThat(traces.getExported())
            .extracting(span -> span.getAttributes().get(DB_SYSTEM_NAME), span -> span.getAttributes().get(DB_QUERY_TEXT))
            .containsExactly(
                tuple("h2database", null),
                tuple("h2database", null));
    }

    @Test
    void sampledOutSpanDescribesNoArguments() {
        ext.getJdbi().getConfig(SqlStatements.class).setIncludeBindingsInTelemetry(true);
        ext.getJdbi().installPlugin(new JdbiOpenTelemetryPlugin(sdk(SimpleSpanProcessor.create(traces), Sampler.alwaysOff())));
        final CountingBean bean = new CountingBean();

        try (Handle handle = ext.getJdbi().open()) {
            assertThat(handle.createQuery("select cast(:name as varchar)").bindBean(bean).mapTo(String.class).one())
                .isEqualTo("Alice");
        }

        assertThat(bean.reads).hasValue(1);
        assertThat(traces.getExported()).isEmpty();
    }

    public static final class CountingBean {
        private final AtomicInteger reads = new AtomicInteger();

        public String getName() {
            reads.incrementAndGet();
            return "Alice";
        }
    }

    private static List<Integer> selectTwice(final Jdbi jdbi) {
        try (Handle handle = jdbi.open()) {
            return List.of(
                handle.createQuery(SELECT).mapTo(Integer.class).one(),
                handle.createQuery(SELECT).mapTo(Integer.class).one());
        }
    }

    private static OpenTelemetry sdk(final SpanProcessor processor, final Sampler sampler) {
        return OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setSampler(sampler)
                .build())
            .build();
    }

    private Jdbi jdbi(final Function<Connection, DatabaseMetaData> metaData) {
        return Jdbi.create(() -> {
            final Connection connection = DriverManager.getConnection(ext.getUrl());
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Connection.class},
                (proxy, method, args) -> {
                    if ("getMetaData".equals(method.getName())) {
                        metaDataCalls.incrementAndGet();
                        return metaData.apply(connection);
                    }
                    return invoke(connection, method, args);
                });
        });
    }

    private static Object invoke(final Connection connection, final Method method, final Object[] args) throws Throwable {
        try {
            return method.invoke(connection, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
